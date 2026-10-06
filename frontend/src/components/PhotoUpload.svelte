<script>
  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher();
  let preview;

  function onPick(e) {
    const files = Array.from(e.target.files || []);
    preview = files.map((f) => ({ file: f, url: URL.createObjectURL(f), name: f.name }));
    dispatch('change', files);
  }
</script>

<div class="row">
  <label>现场照片（可多选，本地保存）</label>
  <input type="file" accept="image/*" multiple on:change={onPick} />
  {#if preview && preview.length}
    <div class="photos" style="margin-top:8px">
      {#each preview as p}
        <div class="photo">
          <img src={p.url} alt={p.name} on:click={() => dispatch('zoom', p.url)} />
          <div class="cap">{p.name}</div>
        </div>
      {/each}
    </div>
  {/if}
</div>
