#!/bin/sh

DIST="kiwipicker"
BINARY="kiwipicker"
ZIP="kiwipicker.zip"
BACKEND_DIR="backend"
FRONTEND_DIR="frontend"
START_SCRIPT="start"

if [[ ! -z ${BUILD_WINDOWS+x} ]]; then
    export GOOS=windows
    export GOARCH=amd64
    export CGO_ENABLED=1
    export CC=x86_64-w64-mingw32-gcc
    BINARY="$BINARY.exe"
    START_SCRIPT="$START_SCRIPT.bat"
else
    START_SCRIPT="$START_SCRIPT.sh"
fi

set -eu

rm -rf $DIST
rm -f $ZIP
mkdir $DIST
mkdir $DIST/static

echo "Copying startup script..."
cp $START_SCRIPT $DIST

echo "Building backend..."
pushd $BACKEND_DIR
go build -ldflags="-s -w" -o $BINARY
mv $BINARY ../$DIST
popd

echo "Building frontend..."
pushd $FRONTEND_DIR
npm run build
mv dist/* ../$DIST/static
popd

echo "Creating zip file..."
zip -r $ZIP $DIST/*
