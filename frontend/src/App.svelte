<script>
    import { onMount } from 'svelte';
    import Station from './station';

    const apiURL = "http://localhost:3000"

    let stations = null;
    let error = null;

    async function loadStations() {
        const res = await fetch(apiURL + '/stations');
        if (res.ok) {
        stations = await res.json();
        } else {
        console.error('Failed to fetch stations');
        }
    }

    onMount(loadStations);

    let name = '';
    let url = '';

    async function handleSubmit(event) {
      event.preventDefault(); // prevent default form submission

      const station = { name, url };

      try {
        const res = await fetch(apiURL + '/stations', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(station)
        });

        if (!res.ok) {
          error = `Error: ${res.status}`;
          return;
        }

        const data = await res.json();
        name = '';
        url = '';
        loadStations();
      } catch (err) {
        error = 'Failed to send request';
      }
    }

    async function deleteStation(id) {
      if (!confirm('Are you sure you want to delete this station?')) return;

      try {
        const res = await fetch(`${apiURL}/stations/${id}`, {
          method: 'DELETE'
        });

        if (!res.ok) {
          alert(`Failed to delete (status ${res.status})`);
          return;
        }

        // optionally reload data or update UI
        loadStations();
      } catch (err) {
        alert('Error deleting station');
      }
    }
</script>

<main>
    <h1 class="text-3xl text-center mt-4 font-bold">Kiwipicker</h1>
    <div class="my-4 flex justify-center">
        <form method="post" action={apiURL + '/stations'} on:submit|preventDefault={handleSubmit}>
            <label for="name">Name</label>
            <input id="name" type="text" bind:value={name} class="border-2 border-neutral-600 p-1 rounded-sm bg-white" required />
            <label for="url">URL</label>
            <input id="url" type="url" bind:value={url} class="border-2 border-neutral-500 p-1 rounded-sm"  required/>
            <button type="submit" class="p-2 bg-blue-700 text-white rounded-sm border-2 border-blue-900">
                Add
            </button>
        </form>
    </div>
    <div class="p-4">
        {#if error}
          <p class="font-bold text-red-500">{error}</p>
        {:else if stations === null}
          <p>Loading stations...</p>
        {:else}
            {#if stations.length < 1}
                <p>No stations found.</p>
            {:else}
            <div class="grid xl:grid-cols-5 md:grid-cols-3 gap-2 grid-cols-1">
                {#each stations as station}
                    <div class="relative bg-neutral-400 hover:bg-neutral-300 p-4 w-full h-full rounded-sm min-h-[150px] border border-neutral-900 flex justify-center items-center">
                      <button
                        on:click={() => deleteStation(station.id)}
                        class="absolute top-1 right-1 w-8 h-8 bg-red-500 text-white border-2 border-red-800 flex justify-center items-center rounded-full text-xl cursor-pointer"
                      >
                        x
                      </button>

                      <a href={station.url} target="_blank" class="w-full h-full flex items-center justify-center">
                        <div class="font-bold w-full h-full flex justify-center items-center">
                            <p>
                                {station.name}
                            </p>
                        </div>
                      </a>
                    </div>
                {/each}
            </div>
            {/if}
        {/if}
    </div>
</main>
