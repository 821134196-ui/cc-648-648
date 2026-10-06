<script>
  import { onMount } from 'svelte';
  import { api, STATUS, fmtTime } from '../api.js';
  import StatusBadge from '../components/StatusBadge.svelte';

  let tickets = [];
  let reminders = [];
  let filter = '';
  let error = '';
  let loading = true;

  const FILTERS = [
    ['', '全部'],
    ['REPORTED', '待受理'],
    ['IN_REPAIR', '维修中'],
    ['PENDING_RECHECK', '待复查'],
    ['VERIFIED', '复查通过'],
    ['DISPUTED', '异议处理中'],
  ];

  async function load() {
    loading = true;
    error = '';
    try {
      [tickets, reminders] = await Promise.all([api.tickets(filter), api.reminders()]);
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  onMount(load);

  function setFilter(f) {
    filter = f;
    load();
  }

  $: eligibleCount = reminders.filter((r) => r.eligible).length;
</script>

<div class="page-title">
  <h1>渗水报修工单</h1>
  <a class="btn-primary" href="#/new">＋ 新建报修</a>
</div>

{#if reminders.length}
  <div class="banner {eligibleCount ? 'ok' : 'warn'}">
    <span class="icon">{eligibleCount ? '🌧️' : '⏳'}</span>
    <div>
      天气提醒：{reminders.length} 个工单待复查，
      {#if eligibleCount}
        其中 <b>{eligibleCount} 个已满足雨后复查条件</b>，请尽快安排复查。
      {:else}
        完工后暂无有效降雨，暂不满足复查条件。
      {/if}
      <a href="#/weather">查看天气模拟 →</a>
    </div>
  </div>
{/if}

<div class="filters">
  {#each FILTERS as [value, label]}
    <button class:active={filter === value} on:click={() => setFilter(value)}>{label}</button>
  {/each}
</div>

<div class="card" style="padding: 4px 8px;">
  {#if error}
    <div class="alert error">{error}</div>
  {:else if loading}
    <p class="muted" style="padding: 0 12px;">加载中…</p>
  {:else if !tickets.length}
    <p class="muted" style="padding: 0 12px;">暂无工单</p>
  {:else}
    <table class="list">
      <thead>
        <tr>
          <th>编号</th><th>楼栋 / 立面</th><th>房间</th><th>报修人</th>
          <th>点位</th><th>状态</th><th>报修时间</th><th>标记</th>
        </tr>
      </thead>
      <tbody>
        {#each tickets as t}
          <tr on:click={() => (location.hash = `#/ticket/${t.id}`)}>
            <td>#{t.id}</td>
            <td>{t.building} · {t.facade}</td>
            <td>{t.room}</td>
            <td>{t.residentName}</td>
            <td class="muted small">{t.pointNote}</td>
            <td><StatusBadge status={t.status} /></td>
            <td class="muted small">{fmtTime(t.createdAt)}</td>
            <td class="small">
              {#if t.recurrence}<span class="badge st-disputed">复发</span>{/if}
              {#if t.hasRecurrences}<span class="badge st-pending">有复发</span>{/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>
