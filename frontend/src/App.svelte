<script>
  import { onMount } from 'svelte';
  import { api } from './api.js';
  import WeatherPanel from './components/WeatherPanel.svelte';
  import ReportList from './components/ReportList.svelte';
  import ReportDetail from './components/ReportDetail.svelte';
  import NewReportModal from './components/NewReportModal.svelte';

  let view = { name: 'list' };
  let reports = [];
  let weather = null;
  let showNew = false;
  let detailTick = 0; // 操作后强制刷新详情

  function parseHash() {
    const h = location.hash || '';
    const m = h.match(/^#\/reports\/(\d+)/);
    view = m ? { name: 'detail', id: Number(m[1]) } : { name: 'list' };
  }

  async function loadAll() {
    reports = await api.listReports();
    weather = await api.weather();
  }

  function go(path) {
    location.hash = path;
  }

  onMount(() => {
    parseHash();
    loadAll();
    window.addEventListener('hashchange', parseHash);
  });

  async function afterChanged() {
    await loadAll();
    detailTick++;
  }
</script>

<header class="topbar">
  <h1>🏢 外墙渗水季节复查系统</h1>
  <span class="sub">报修 · 施工 · 雨后复查 · 异议闭环</span>
  <span class="spacer"></span>
  {#if view.name === 'detail'}
    <button class="btn secondary sm" on:click={() => go('/')}>返回列表</button>
  {/if}
  <button class="btn sm" on:click={() => (showNew = true)}>+ 住户报修</button>
</header>

<main class="layout">
  {#if weather}
    <WeatherPanel {weather} on:changed={loadAll} />
  {/if}

  {#if view.name === 'list'}
    <ReportList {reports} {go} />
  {:else}
    <ReportDetail id={view.id} tick={detailTick} {go} on:changed={afterChanged} />
  {/if}
</main>

{#if showNew}
  <NewReportModal
    on:close={() => (showNew = false)}
    on:created={async (e) => {
      showNew = false;
      await loadAll();
      go(`/reports/${e.detail}`);
    }} />
{/if}
