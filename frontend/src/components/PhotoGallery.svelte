<script>
  export let photos = [];
  export let emptyText = '暂无照片';
  let zoom;
</script>

{#if photos.length}
  <div class="photos">
    {#each photos as p}
      <div class="photo">
        <img src={p.url} alt={p.caption || ''} on:click={() => (zoom = p.url)} />
        {#if p.caption}<div class="cap">{p.caption}</div>{/if}
      </div>
    {/each}
  </div>
{:else}
  <div class="muted" style="font-size:12.5px">{emptyText}</div>
{/if}

{#if zoom}
  <div class="lightbox" on:click={() => (zoom = null)} role="dialog">
    <img src={zoom} alt="放大查看" />
  </div>
{/if}
