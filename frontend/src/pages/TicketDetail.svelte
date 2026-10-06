<script>
  import { onMount } from 'svelte';
  import { api, fmtTime, RESOLUTIONS } from '../api.js';
  import StatusBadge from '../components/StatusBadge.svelte';
  import PhotoGrid from '../components/PhotoGrid.svelte';

  export let id;

  let detail = null;
  let points = [];
  let error = '';
  let actionError = '';
  let actionMsg = '';

  // 各操作表单
  let construction = { workerName: '', content: '' };
  let constructionFiles = [];
  let recheck = { inspectorName: '', result: 'PASS', content: '' };
  let recheckFiles = [];
  let disputeForm = { residentName: '', content: '' };
  let replyForm = { reply: '', resolution: 'KEEP_VERIFIED' };
  let recurrenceForm = { residentName: '', phone: '', room: '', description: '' };
  let linkPointId = '';
  let showRecurrence = false;
  let showDispute = false;
  let showLink = false;

  async function load() {
    error = '';
    try {
      [detail, points] = await Promise.all([api.ticket(id), api.points()]);
    } catch (e) {
      error = e.message;
    }
  }

  onMount(load);

  async function act(fn, okMsg) {
    actionError = '';
    actionMsg = '';
    try {
      await fn();
      actionMsg = okMsg;
      await load();
    } catch (e) {
      actionError = e.message;
    }
  }

  const startRepair = () => act(() => api.startRepair(id), '已开始维修');

  const submitConstruction = () =>
    act(async () => {
      await api.completeConstruction(id, construction);
      if (constructionFiles.length) {
        await api.uploadPhotos(id, 'CONSTRUCTION', constructionFiles);
      }
      construction = { workerName: '', content: '' };
      constructionFiles = [];
    }, '施工完成，工单进入待复查（需等待有效降雨）');

  const submitRecheck = () =>
    act(async () => {
      await api.recheck(id, recheck);
      if (recheckFiles.length) {
        await api.uploadPhotos(id, 'RECHECK', recheckFiles);
      }
      recheck = { inspectorName: '', result: 'PASS', content: '' };
      recheckFiles = [];
    }, '复查已记录');

  const submitLink = () =>
    act(async () => {
      await api.linkPoint(id, Number(linkPointId));
      showLink = false;
    }, '已关联到点位');

  const submitRecurrence = () =>
    act(async () => {
      const created = await api.recurrence(id, recurrenceForm);
      location.hash = `#/ticket/${created.id}`;
    }, '已登记复发报修');

  const submitDispute = () =>
    act(async () => {
      await api.dispute(id, disputeForm);
      disputeForm = { residentName: '', content: '' };
      showDispute = false;
    }, '异议已提交，等待物业处理');

  const submitReply = (disputeId) =>
    act(() => api.replyDispute(id, disputeId, replyForm), '异议已回复');

  $: otherPoints = detail ? points.filter((p) => p.id !== detail.point.id) : [];
</script>

{#if error}
  <div class="card"><div class="alert error">{error}</div></div>
{:else if !detail}
  <div class="card"><p class="muted">加载中…</p></div>
{:else}
  <div class="page-title">
    <h1>工单 #{detail.id} · {detail.building} {detail.facade} {detail.room}</h1>
    <StatusBadge status={detail.status} />
  </div>

  {#if actionMsg}<div class="alert success">{actionMsg}</div>{/if}
  {#if actionError}<div class="alert error">{actionError}</div>{/if}

  <!-- 基本信息 -->
  <div class="card">
    <div class="meta-grid">
      <div><div class="k">报修人</div><div class="v">{detail.residentName}</div></div>
      <div><div class="k">联系电话</div><div class="v">{detail.phone || '—'}</div></div>
      <div><div class="k">报修时间</div><div class="v">{fmtTime(detail.createdAt)}</div></div>
      <div><div class="k">渗水点位</div><div class="v">{detail.point.note}</div></div>
    </div>

    {#if detail.recurrenceOf}
      <div class="alert error mt8">
        ⚠️ 本工单为 <a href="#/ticket/{detail.recurrenceOf.id}">#{detail.recurrenceOf.id}</a>
        的<b>复发报修</b>（原点位修复后再次漏水），此前处理过程见原工单。
      </div>
    {/if}
    {#if detail.recurrences.length}
      <div class="mt8 small">
        该点位修复后再次漏水 {detail.recurrences.length} 次：
        {#each detail.recurrences as r}
          <a class="chip" href="#/ticket/{r.id}">#{r.id} {r.room}（复发）</a>
        {/each}
      </div>
    {/if}

    <div class="mt8">
      <div class="small muted">其他同点位工单（{detail.point.tickets.length} 户，各户记录独立保留）：</div>
      <div class="chips mt8">
        {#each detail.point.tickets as t}
          <a class="chip" href="#/ticket/{t.id}">
            #{t.id} {t.room} {t.residentName} <StatusBadge status={t.status} />
          </a>
        {:else}
          <span class="muted small">暂无其他住户报修同一点位</span>
        {/each}
      </div>
    </div>
  </div>

  <!-- 复查资格（待复查时突出显示） -->
  {#if detail.status === 'PENDING_RECHECK'}
    <div class="eligibility {detail.eligibility.eligible ? 'ok' : 'warn'}">
      <div class="title">
        {detail.eligibility.eligible ? '✅ 已满足雨后复查条件' : '⏳ 未达到雨后复查条件，暂不能标记验证通过'}
      </div>
      <div>{detail.eligibility.reason}</div>
      <div class="small muted mt8">
        完工日期：{detail.eligibility.completedAt || '—'} · 模拟当前日期：{detail.eligibility.currentDate}
        · 有效降雨阈值 ≥{detail.eligibility.thresholdMm}mm
      </div>
      {#if detail.eligibility.effectiveRains.length}
        <ul>
          {#each detail.eligibility.effectiveRains as r}
            <li>{r.date} 降雨 {r.rainfallMm}mm{#if r.note}（{r.note}）{/if}</li>
          {/each}
        </ul>
      {/if}
    </div>
  {/if}

  <!-- 时间线：报修 / 施工 / 复查 -->
  <div class="card">
    <div class="timeline">
      <div class="tl-section">
        <span class="tl-dot"></span>
        <h3>① 报修</h3>
        <div class="tl-entry">
          <div class="head"><span class="who">{detail.residentName}</span><span class="when">{fmtTime(detail.createdAt)}</span></div>
          <div class="body">{detail.description || '（无描述）'}</div>
          <PhotoGrid photos={detail.reportPhotos} />
        </div>
      </div>

      <div class="tl-section">
        <span class="tl-dot"></span>
        <h3>② 施工</h3>
        {#each detail.constructions as c}
          <div class="tl-entry">
            <div class="head"><span class="who">{c.workerName}</span><span class="when">完工于 {c.completedAt}</span></div>
            <div class="body">{c.content}</div>
          </div>
        {:else}
          <div class="tl-empty">暂无施工记录</div>
        {/each}
        <PhotoGrid photos={detail.constructionPhotos} />
      </div>

      <div class="tl-section">
        <span class="tl-dot"></span>
        <h3>③ 复查（雨后）</h3>
        {#each detail.rechecks as r}
          <div class="tl-entry">
            <div class="head">
              <span class="who">{r.inspectorName}</span>
              <span class="when">复查于 {r.checkedAt}</span>
              <span class="badge {r.result === 'PASS' ? 'badge-pass' : 'badge-fail'}">
                {r.result === 'PASS' ? '复查通过' : '复查未通过'}
              </span>
            </div>
            <div class="body">{r.content}</div>
            {#if r.rainDate}
              <div class="small muted">依据降雨：{r.rainDate}（{r.rainMm}mm）</div>
            {/if}
          </div>
        {:else}
          <div class="tl-empty">暂无复查记录 —— 需等待完工后的有效降雨</div>
        {/each}
        <PhotoGrid photos={detail.recheckPhotos} />
      </div>
    </div>
  </div>

  <!-- 异议记录 -->
  {#if detail.disputes.length}
    <div class="card">
      <h2>住户异议与物业回复</h2>
      {#each detail.disputes as d}
        <div class="tl-entry dispute {d.status === 'REPLIED' ? 'replied' : ''}">
          <div class="head">
            <span class="who">{d.residentName}</span>
            <span class="when">{fmtTime(d.createdAt)}</span>
            <span class="badge {d.status === 'OPEN' ? 'st-disputed' : 'st-verified'}">
              {d.status === 'OPEN' ? '待物业回复' : '已回复'}
            </span>
          </div>
          <div class="body">{d.content}</div>
          {#if d.reply}
            <div class="mt8 small">
              <b>物业回复（{fmtTime(d.repliedAt)} · {RESOLUTIONS[d.resolution]}）：</b>{d.reply}
            </div>
          {/if}
        </div>
      {/each}
    </div>
  {/if}

  <!-- 操作区 -->
  <div class="section-title">工单操作</div>
  <div class="actions-grid">
    {#if detail.status === 'REPORTED'}
      <div class="action-box">
        <h3>🔧 受理派工</h3>
        <p class="hint">确认报修并开始维修，工单进入“维修中”。</p>
        <button class="btn-primary" on:click={startRepair}>开始维修</button>
      </div>
    {/if}

    {#if detail.status === 'IN_REPAIR'}
      <div class="action-box">
        <h3>🧱 登记施工并完工</h3>
        <p class="hint">完工后进入“待复查”，需等待完工后的有效降雨才能复查。</p>
        <form class="stack" on:submit|preventDefault={submitConstruction}>
          <input type="text" placeholder="施工人员 *" bind:value={construction.workerName} required />
          <textarea placeholder="施工内容" bind:value={construction.content}></textarea>
          <input type="file" accept="image/*" multiple
                 on:change={(e) => (constructionFiles = Array.from(e.target.files || []))} />
          <button class="btn-primary" type="submit">完工，转入待复查</button>
        </form>
      </div>
    {/if}

    {#if detail.status === 'PENDING_RECHECK'}
      <div class="action-box">
        <h3>🔍 提交雨后复查</h3>
        {#if detail.eligibility.eligible}
          <p class="hint">已满足雨后复查条件，可到现场复查并记录结论。</p>
        {:else}
          <p class="hint">⚠️ {detail.eligibility.reason}。提交将被系统拒绝。</p>
        {/if}
        <form class="stack" on:submit|preventDefault={submitRecheck}>
          <input type="text" placeholder="复查人员 *" bind:value={recheck.inspectorName} required />
          <select bind:value={recheck.result}>
            <option value="PASS">复查通过（无渗漏）</option>
            <option value="FAIL">复查未通过（仍有渗漏，返修）</option>
          </select>
          <textarea placeholder="复查情况" bind:value={recheck.content}></textarea>
          <input type="file" accept="image/*" multiple
                 on:change={(e) => (recheckFiles = Array.from(e.target.files || []))} />
          <button class="btn-primary" type="submit" disabled={!detail.eligibility.eligible}>
            提交复查结论
          </button>
        </form>
      </div>
    {/if}

    {#if detail.status === 'VERIFIED'}
      <div class="action-box">
        <h3>🔁 登记复发报修</h3>
        <p class="hint">原点位修复后再次漏水时登记，新工单关联本工单，保留此前处理过程。</p>
        {#if showRecurrence}
          <form class="stack" on:submit|preventDefault={submitRecurrence}>
            <input type="text" placeholder="报修人 *" bind:value={recurrenceForm.residentName} required />
            <input type="text" placeholder="房间号 *" bind:value={recurrenceForm.room} required />
            <input type="tel" placeholder="联系电话" bind:value={recurrenceForm.phone} />
            <textarea placeholder="复发情况描述" bind:value={recurrenceForm.description}></textarea>
            <div class="flex">
              <button class="btn-primary" type="submit">提交复发报修</button>
              <button class="btn" type="button" on:click={() => (showRecurrence = false)}>取消</button>
            </div>
          </form>
        {:else}
          <button class="btn-danger" on:click={() => (showRecurrence = true)}>登记复发</button>
        {/if}
      </div>

      <div class="action-box">
        <h3>✋ 住户提出异议</h3>
        <p class="hint">住户不认可复查结论时提交，由物业继续处理并记录回复。</p>
        {#if showDispute}
          <form class="stack" on:submit|preventDefault={submitDispute}>
            <input type="text" placeholder="异议人（默认报修人）" bind:value={disputeForm.residentName} />
            <textarea placeholder="异议内容 *" bind:value={disputeForm.content} required></textarea>
            <div class="flex">
              <button class="btn-primary" type="submit">提交异议</button>
              <button class="btn" type="button" on:click={() => (showDispute = false)}>取消</button>
            </div>
          </form>
        {:else}
          <button class="btn" on:click={() => (showDispute = true)}>提出异议</button>
        {/if}
      </div>
    {/if}

    {#if detail.status === 'DISPUTED'}
      {#each detail.disputes.filter((d) => d.status === 'OPEN') as d}
        <div class="action-box">
          <h3>💬 回复异议（{d.residentName}）</h3>
          <p class="hint">{d.content}</p>
          <form class="stack" on:submit|preventDefault={() => submitReply(d.id)}>
            <textarea placeholder="回复内容 *" bind:value={replyForm.reply} required></textarea>
            <select bind:value={replyForm.resolution}>
              {#each Object.entries(RESOLUTIONS) as [value, label]}
                <option {value}>{label}</option>
              {/each}
            </select>
            <button class="btn-primary" type="submit">提交回复</button>
          </form>
        </div>
      {/each}
    {/if}

    <div class="action-box">
      <h3>🔗 同点位关联</h3>
      <p class="hint">多户报修同一渗水点时关联到同一点位，各户记录独立保留。</p>
      {#if showLink}
        <form class="stack" on:submit|preventDefault={submitLink}>
          <select bind:value={linkPointId} required>
            <option value="" disabled>选择目标点位</option>
            {#each otherPoints as p}
              <option value={p.id}>{p.building} · {p.facade} · {p.note}（{p.tickets.length} 条工单）</option>
            {/each}
          </select>
          <div class="flex">
            <button class="btn-primary" type="submit" disabled={!linkPointId}>关联</button>
            <button class="btn" type="button" on:click={() => (showLink = false)}>取消</button>
          </div>
        </form>
      {:else}
        <button class="btn" on:click={() => (showLink = true)} disabled={!otherPoints.length}>
          {otherPoints.length ? '关联到其他点位' : '暂无其他点位可关联'}
        </button>
      {/if}
    </div>
  </div>

  <p class="mt16"><a href="#/">← 返回工单列表</a></p>
{/if}
