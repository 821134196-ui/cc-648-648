<script>
  import { onMount } from 'svelte';
  import { api } from '../api.js';
  import StatusBadge from '../components/StatusBadge.svelte';

  let points = [];
  let error = '';

  onMount(async () => {
    try {
      points = await api.points();
    } catch (e) {
      error = e.message;
    }
  });
</script>

<div class="page-title">
  <h1>渗水点位</h1>
  <span class="muted small">同一渗水点被多户报修时关联到同一点位，各住户记录独立保留</span>
</div>

{#if error}
  <div class="card"><div class="alert error">{error}</div></div>
{/if}

{#each points as p}
  <div class="card">
    <div class="point-head">
      <div>
        <div class="loc">{p.building} · {p.facade}</div>
        <div class="note">{p.note}</div>
      </div>
      <span class="badge st-repair">{p.tickets.length} 条工单</span>
    </div>
    <table class="list mt8">
      <thead>
        <tr><th>工单</th><th>房间</th><th>报修人</th><th>状态</th><th></th></tr>
      </thead>
      <tbody>
        {#each p.tickets as t}
          <tr on:click={() => (location.hash = `#/ticket/${t.id}`)}>
            <td>#{t.id}</td>
            <td>{t.room}</td>
            <td>{t.residentName}</td>
            <td><StatusBadge status={t.status} /></td>
            <td><a href="#/ticket/{t.id}">查看 →</a></td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{:else}
  {#if !error}<div class="card"><p class="muted">暂无点位</p></div>{/if}
{/each}
