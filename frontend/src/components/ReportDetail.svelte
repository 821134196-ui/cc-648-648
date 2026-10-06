<script>
  import { api, fmtDateTime } from '../api.js';
  import PhotoGallery from './PhotoGallery.svelte';
  import RepairModal from './RepairModal.svelte';
  import RecheckModal from './RecheckModal.svelte';
  import ObjectionModal from './ObjectionModal.svelte';
  import ReplyObjectionModal from './ReplyObjectionModal.svelte';

  export let id;
  export let tick = 0;
  export let go;
  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher();

  let d = null;
  let modal = null;
  let replyObjectionId = null;
  let loadErr = '';

  async function load() {
    loadErr = '';
    try {
      d = await api.getReport(id);
    } catch (e) {
      loadErr = e.message;
    }
  }

  // id/tick 变化（含首次）时加载详情
  $: if (id || tick) load();

  // 合并时间线：报修 → 施工 → 复查 → 异议，按时间排序
  $: timeline = d ? buildTimeline(d) : [];

  function buildTimeline(detail) {
    const created = new Date(detail.report.createdAt).getTime();
    const items = [];
    items.push({
      kind: 'report',
      time: detail.report.createdAt,
      data: detail.report,
      photos: detail.reportPhotos
    });
    for (const r of detail.repairs) {
      items.push({
        kind: 'repair',
        time: r.happenedAt,
        data: r,
        photos: r.photos,
        previousRound: new Date(r.happenedAt).getTime() < created
      });
    }
    for (const r of detail.rechecks) {
      items.push({
        kind: 'recheck',
        time: r.createdAt,
        data: r,
        photos: r.photos,
        result: r.result
      });
    }
    for (const o of detail.objections) {
      items.push({ kind: 'objection', time: o.createdAt, data: o, photos: o.photos });
    }
    return items.sort((a, b) => new Date(a.time) - new Date(b.time));
  }

  async function done() {
    modal = null;
    await load();
    dispatch('changed');
  }

  $: pendingObjection = d?.objections.find((o) => o.outcome === 'PENDING');
</script>

{#if loadErr}
  <div class="panel"><div class="form-error">{loadErr}</div></div>
{:else if d}
  {@const r = d.report}

  <!-- 顶部信息 -->
  <div class="panel">
    <a class="back-link" href="#/">← 返回报修列表</a>
    <div style="display:flex;align-items:flex-start;gap:14px;flex-wrap:wrap">
      <div style="flex:1;min-width:300px">
        <h2 style="margin:0 0 6px;font-size:17px">
          {r.building} · {r.facade} · {r.location}
        </h2>
        <dl class="kv">
          <dt>房间</dt><dd>{r.room}（{r.residentName}{r.contact ? ' · ' + r.contact : ''}）</dd>
          <dt>报修时间</dt><dd>{fmtDateTime(r.createdAt)}</dd>
          {#if r.description}
            <dt>报修描述</dt><dd>{r.description}</dd>
          {/if}
        </dl>
      </div>
      <div style="text-align:right">
        <span class="badge {r.status}" style="font-size:13px;padding:4px 14px">{r.statusLabel}</span>
        {#if r.recurrence}
          <div style="margin-top:8px">
            <span class="badge tag recur">🔁 原问题复发 · 第{r.recurrenceNo}次报修</span>
          </div>
        {/if}
        <div style="margin-top:8px">
          <span class="badge spot-{r.spotStatus}">点位：{r.spotStatus}</span>
        </div>
      </div>
    </div>

    <!-- 雨后复查条件 -->
    {#if r.status === 'PENDING_RECHECK'}
      <div class="rain-box {r.rainEligible ? 'ok' : 'no'}">
        {#if r.rainEligible}
          ✅ 已具备雨后复查条件：{r.rainMessage}
        {:else}
          ⛔ {r.rainMessage}
        {/if}
      </div>
    {/if}

    <!-- 操作区 -->
    <div class="actions">
      {#if r.status === 'PENDING_REPAIR'}
        <button class="btn" on:click={() => (modal = 'repair')}>🔧 记录施工</button>
      {:else if r.status === 'PENDING_RECHECK'}
        <button class="btn" on:click={() => (modal = 'recheck')}>🔎 雨后复查</button>
      {:else if r.status === 'VERIFIED'}
        <button class="btn danger" on:click={() => (modal = 'objection')}>🙋 住户不认可，提出异议</button>
      {:else if r.status === 'OBJECTIONED'}
        {#if pendingObjection}
          <button class="btn" on:click={() => (replyObjectionId = pendingObjection.id)}>
            📝 物业回复异议
          </button>
          <span class="muted" style="font-size:12.5px;align-self:center">住户异议等待物业处理</span>
        {/if}
      {/if}
    </div>
  </div>

  <!-- 同点位关联 / 复发链 -->
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
    <div class="panel" style="margin-bottom:0">
      <h2>🔗 同点位关联报修 <span class="hint">同一物理渗水点联合维修，各户记录独立保留</span></h2>
      <div class="linked-list">
        <div class="linked-item" style="background:#f7faff">
          <span class="grow"><b>本户</b> · {r.room} · {r.residentName}</span>
          <span class="badge {r.status}">{r.statusLabel}</span>
        </div>
        {#each d.linkedReports as x}
          <a class="linked-item" href="#/reports/{x.id}" style="color:inherit">
            <span class="grow">{x.room} · {x.residentName}</span>
            {#if x.recurrenceNo > 1}<span class="badge tag recur">第{x.recurrenceNo}次</span>{/if}
            <span class="badge {x.status}">{x.statusLabel}</span>
            <span class="muted" style="font-size:11.5px">›</span>
          </a>
        {:else}
          <div class="muted" style="font-size:12.5px">该点位目前只有本户报修</div>
        {/each}
      </div>
    </div>

    <div class="panel" style="margin-bottom:0">
      <h2>🔁 本户报修历史 <span class="hint">复发时可查看此前处理过程</span></h2>
      <div class="linked-list">
        {#each d.recurrenceChain as x}
          <a class="linked-item" href="#/reports/{x.id}" style="color:inherit">
            <span class="grow">
              {x.id === r.id ? '当前单' : '历史单'} # {x.id}
              {#if x.recurrenceNo === 1}（首次报修）{:else}（第{x.recurrenceNo}次，复发）{/if}
            </span>
            {#if x.reopenedAfterResolved}<span class="badge tag recur">解决后再漏</span>{/if}
            <span class="badge {x.status}">{x.statusLabel}</span>
          </a>
        {/each}
      </div>
    </div>
  </div>

  <!-- 时间线 -->
  <div class="panel" style="margin-top:16px">
    <h2>处理过程时间线 <span class="hint">按 报修 → 施工 → 复查 → 异议 排列照片与结论</span></h2>
    <div class="timeline">
      {#each timeline as item}
        {#if item.kind === 'report'}
          <div class="tl-item report">
            <div class="tl-head">
              住户报修 <span class="tl-time">{fmtDateTime(item.time)}</span>
            </div>
            {#if item.data.description}<div class="tl-body">{item.data.description}</div>{/if}
            <PhotoGallery photos={item.photos} emptyText="报修时未上传照片" />
          </div>
        {:else if item.kind === 'repair'}
          <div class="tl-item repair">
            <div class="tl-head">
              维修施工
              {#if item.previousRound}<span class="badge tag">上一轮处理</span>{/if}
              <span class="tl-time">完成于 {fmtDateTime(item.time)}</span>
            </div>
            <div class="tl-body">
              维修员：{item.data.workerName}{#if item.data.summary}　{item.data.summary}{/if}
            </div>
            <PhotoGallery photos={item.photos} emptyText="施工时未上传照片" />
          </div>
        {:else if item.kind === 'recheck'}
          <div class="tl-item recheck {item.result === 'DRY' ? 'dry' : 'leak'}">
            <div class="tl-head">
              雨后复查 · {item.data.resultLabel}
              <span class="tl-time">{fmtDateTime(item.time)}</span>
            </div>
            <div class="tl-meta">
              复查人员：{item.data.inspectorName} ·
              依据 {item.data.qualifyingRainDate} 的 {item.data.qualifyingRainMm}mm 有效降雨
            </div>
            {#if item.data.conclusion}<div class="tl-body">{item.data.conclusion}</div>{/if}
            <PhotoGallery photos={item.photos} emptyText="复查时未上传照片" />
          </div>
        {:else}
          <div class="tl-item objection">
            <div class="tl-head">
              住户异议 · {item.data.outcomeLabel}
              <span class="tl-time">{fmtDateTime(item.time)}</span>
            </div>
            <div class="tl-body">理由：{item.data.reason}</div>
            <PhotoGallery photos={item.photos} emptyText="异议未附照片" />
            {#if item.data.outcome !== 'PENDING'}
              <div class="callout" style="margin-top:8px">
                物业回复（{item.data.repliedBy || '物业'}，{fmtDateTime(item.data.repliedAt)}）：
                {item.data.replyNote}
              </div>
            {:else}
              <div class="callout warn" style="margin-top:8px">等待物业回复处理</div>
            {/if}
          </div>
        {/if}
      {/each}
    </div>
  </div>

  <!-- 弹窗 -->
  {#if modal === 'repair'}
    <RepairModal reportId={r.id} on:close={() => (modal = null)} on:done={done} />
  {:else if modal === 'recheck'}
    <RecheckModal
      reportId={r.id}
      eligible={r.rainEligible}
      rainMessage={r.rainMessage}
      on:close={() => (modal = null)}
      on:done={done} />
  {:else if modal === 'objection'}
    <ObjectionModal reportId={r.id} on:close={() => (modal = null)} on:done={done} />
  {/if}
  {#if replyObjectionId}
    <ReplyObjectionModal
      objectionId={replyObjectionId}
      on:close={() => (replyObjectionId = null)}
      on:done={() => { replyObjectionId = null; done(); }} />
  {/if}
{/if}
