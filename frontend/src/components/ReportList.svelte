<script>
  export let reports = [];
  export let go;

  let tab = 'all';
  let q = '';

  $: filtered = reports.filter((r) => {
    if (tab !== 'all' && r.status !== tab) return false;
    if (!q.trim()) return true;
    const s = `${r.building} ${r.facade} ${r.location} ${r.room} ${r.residentName}`;
    return s.toLowerCase().includes(q.trim().toLowerCase());
  });

  const tabs = [
    { k: 'all', label: '全部' },
    { k: 'PENDING_REPAIR', label: '待维修' },
    { k: 'PENDING_RECHECK', label: '待复查' },
    { k: 'VERIFIED', label: '已验证' },
    { k: 'OBJECTIONED', label: '异议中' }
  ];
</script>

<div class="panel">
  <div class="toolbar">
    <div class="tabs">
      {#each tabs as t}
        <button class={tab === t.k ? 'active' : ''} on:click={() => (tab = t.k)}>
          {t.label}
        </button>
      {/each}
    </div>
    <span class="grow"></span>
    <input type="text" bind:value={q} placeholder="搜索楼栋/立面/位置/房号/住户"
           style="width:260px" />
  </div>

  <table class="grid">
    <thead>
      <tr>
        <th>位置</th>
        <th>房号 / 住户</th>
        <th>状态</th>
        <th>雨后复查条件</th>
        <th>报修时间</th>
      </tr>
    </thead>
    <tbody>
      {#each filtered as r (r.id)}
        <tr class="clickable" on:click={() => go(`/reports/${r.id}`)}>
          <td>
            <b>{r.building}</b> · {r.facade} · {r.location}
            {#if r.recurrence}
              <span class="badge tag recur">复发第{r.recurrenceNo}次{#if r.reopenedAfterResolved}·已解决后再漏{/if}</span>
            {/if}
          </td>
          <td>{r.room}<br /><span class="muted">{r.residentName}</span></td>
          <td>
            <span class="badge {r.status}">{r.statusLabel}</span>
            <div class="muted" style="font-size:11.5px;margin-top:2px">点位：{r.spotStatus}</div>
          </td>
          <td style="max-width:340px">
            {#if r.status === 'PENDING_RECHECK'}
              <span style="font-size:12.5px;color:{r.rainEligible ? 'var(--green)' : 'var(--amber)'}">
                {r.rainEligible ? '✅ 已具备复查条件' : '⛔ 等待有效降雨'}
              </span>
              <div class="muted" style="font-size:11.5px;margin-top:2px">
                {r.rainMessage}
              </div>
            {:else if r.status === 'VERIFIED'}
              <span class="muted" style="font-size:12px">
                依据 {r.qualifyingRainDate} 的 {r.qualifyingRainMm}mm 降雨验证
              </span>
            {:else}
              <span class="muted" style="font-size:12px">—</span>
            {/if}
          </td>
          <td class="muted" style="font-size:12.5px">{r.createdAt.replace('T', ' ').slice(0, 16)}</td>
        </tr>
      {:else}
        <tr><td colspan="5" class="empty">没有符合条件的报修单</td></tr>
      {/each}
    </tbody>
  </table>
</div>
