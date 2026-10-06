<script>
  export let photos = [];
</script>

{#if photos.length}
  <div class="photo-grid">
    {#each photos as p}
      <div>
        <a href={p.url} target="_blank" rel="noopener">
          <img src={p.url} alt={p.label || '照片'} loading="lazy" />
        </a>
        {#if p.label}<div class="cap" title={p.label}>{p.label}</div>{/if}
      </div>
    {/each}
  </div>
{/if}
