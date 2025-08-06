package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"io"
	"os"
	"sync"
	"path/filepath"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

type Station struct {
	ID   uint   `json:"id" gorm:"primaryKey"`
	Name string `json:"name"`
	URL  string `json:"url"`
}

var (
    db     *gorm.DB
    dbLock sync.Mutex
)

const stationsFile = "stations.db"
const tmpFile = "tmp.db"

func main() {
	var err error
	db, err = gorm.Open(sqlite.Open(stationsFile), &gorm.Config{})
	if err != nil {
		log.Fatal("failed to connect to database:", err)
	}

	if err := db.AutoMigrate(&Station{}); err != nil {
		log.Fatal("migration failed:", err)
	}

	mux := http.NewServeMux()

	// API routes
	mux.HandleFunc("/stations", stationsHandler)
	mux.HandleFunc("/stations/", stationByIDHandler)
	mux.HandleFunc("/" + stationsFile, getStationsFile)

	// Catch-all fallback to static files
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		path := filepath.Join("static", r.URL.Path)

		// If file exists, serve it
		if info, err := os.Stat(path); err == nil && !info.IsDir() {
			http.ServeFile(w, r, path)
			return
		}

		// Otherwise, serve index.html (SPA fallback)
		http.ServeFile(w, r, "static/index.html")
	})

	log.Println("Listening on http://localhost:3000")
	log.Fatal(http.ListenAndServe(":3000", mux))
}

func isFileValidSQLiteDB(path string) bool {
    result := false

    db, err := gorm.Open(sqlite.Open(path), &gorm.Config{})
    if err == nil {
        var count int64

        err = db.Raw("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='stations'").Scan(&count).Error
        if err == nil && count == 1{
            result = true
        }
    }

    return result
}

func reloadDatabase() error {
    dbLock.Lock()
    defer dbLock.Unlock()

    newDB, err := gorm.Open(sqlite.Open(stationsFile), &gorm.Config{})
    if err != nil {
        return err
    }

    db = newDB
    return nil
}

func getStationsFile(w http.ResponseWriter, r *http.Request) {
    switch r.Method {
    case http.MethodGet:
        http.ServeFile(w, r, stationsFile);
    case http.MethodPost:
        file, _, err := r.FormFile("file")
        if err != nil {
            http.Error(w, "Missing database file in upload.", http.StatusBadRequest)
            return
        }

        out, err := os.Create(tmpFile)
        if err != nil {
            http.Error(w, "Could not create temporary data file.", http.StatusInternalServerError)
            return
        }

        _, err = io.Copy(out, file)
        if err != nil {
            http.Error(w, "Could not create read request to temporary data file.", http.StatusInternalServerError)
            return
        }
        out.Close()

        if !isFileValidSQLiteDB(tmpFile) {
            http.Error(w, "File is not a valid database file.", http.StatusBadRequest);
            return;
        }

        // replace the current database file
        os.Rename(stationsFile, stationsFile + ".old")
        os.Rename(tmpFile, stationsFile)

        err = reloadDatabase()
        if err != nil {
            http.Error(w, "Could not reload database. Please restart the application manually.", http.StatusInternalServerError)
        }

        w.WriteHeader(http.StatusNoContent)
        default:
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
    }
}

func serveIndex(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	http.ServeFile(w, r, "static/index.html")
}

func enableCORS(w http.ResponseWriter) {
    w.Header().Set("Access-Control-Allow-Origin", "*") // allow all origins
    w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
    w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
}

// Handle GET /stations and POST /stations
func stationsHandler(w http.ResponseWriter, r *http.Request) {
    enableCORS(w)

    if r.Method == http.MethodOptions {
        w.WriteHeader(http.StatusOK)
        return
    }

	switch r.Method {
	case http.MethodGet:
		var stations []Station
		if err := db.Find(&stations).Error; err != nil {
			http.Error(w, "Failed to fetch stations", http.StatusInternalServerError)
			return
		}
		writeJSON(w, stations, http.StatusOK)

	case http.MethodPost:
		var input Station
		if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
			http.Error(w, "Invalid JSON", http.StatusBadRequest)
			return
		}
		if input.Name == "" || input.URL == "" {
			http.Error(w, "name and url are required", http.StatusBadRequest)
			return
		}
		if err := db.Create(&input).Error; err != nil {
			http.Error(w, "Failed to insert station", http.StatusInternalServerError)
			return
		}
		writeJSON(w, input, http.StatusCreated)

	default:
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}

// Handle GET, PUT, DELETE for /stations/{id}
func stationByIDHandler(w http.ResponseWriter, r *http.Request) {
	idStr := strings.TrimPrefix(r.URL.Path, "/stations/")
	id, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil || id == 0 {
		http.Error(w, "Invalid station ID", http.StatusBadRequest)
		return
	}

	enableCORS(w)

    if r.Method == http.MethodOptions {
        w.WriteHeader(http.StatusOK)
        return
    }

	switch r.Method {
	case http.MethodGet:
		var station Station
		if err := db.First(&station, id).Error; err != nil {
			http.Error(w, "Station not found", http.StatusNotFound)
			return
		}
		writeJSON(w, station, http.StatusOK)

	case http.MethodPut:
		var input Station
		if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
			http.Error(w, "Invalid JSON", http.StatusBadRequest)
			return
		}
		if input.Name == "" || input.URL == "" {
			http.Error(w, "name and url are required", http.StatusBadRequest)
			return
		}

		var station Station
		if err := db.First(&station, id).Error; err != nil {
			http.Error(w, "Station not found", http.StatusNotFound)
			return
		}

		station.Name = input.Name
		station.URL = input.URL

		if err := db.Save(&station).Error; err != nil {
			http.Error(w, "Failed to update station", http.StatusInternalServerError)
			return
		}
		writeJSON(w, station, http.StatusOK)

	case http.MethodDelete:
		if err := db.Delete(&Station{}, id).Error; err != nil {
			http.Error(w, "Failed to delete station", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusNoContent)

	default:
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}

func writeJSON(w http.ResponseWriter, data any, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}
