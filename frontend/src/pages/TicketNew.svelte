<script>
  import { onMount } from 'svelte';
  import { api } from '../api.js';

  let form = {
    building: '',
    facade: '',
    room: '',
    residentName: '',
    phone: '',
    description: '',
    locationNote: '',
    pointId: null,
  };
  let points = [];
  let files = [];
  let error = '';
  let submitting = false;

  onMount(async () => {
    try {
      points = await api.points();
    } catch { /* 点位列表加载失败不阻塞报修 */ }
  });

  function onFiles(e) {
    files = Array.from(e.target.files || []);
  }

  async function submit() {
    error = '';
    submitting = true;
    try {
      const payload = { ...form, pointId: form.pointId ? Number(form.pointId) : null };
      const created = await api.createTicket(payload);
      if (files.length) {
        await api.uploadPhotos(created.id, 'REPORT', files);
      }
      location.hash = `#/ticket/${created.id}`;
    } catch (e) {
      error = e.message;
    } finally {
      submitting = false;
    }
  }
</script>

<div class="page-title"><h1>新建渗水报修</h1></div>

<div class="card">
  <form class="stack" on:submit|preventDefault={submit}>
    <div class="form-grid">
      <div class="form-row">
        <label>楼栋 *</label>
        <input type="text" bind:value={form.building} placeholder="如：3栋" required />
      </div>
      <div class="form-row">
        <label>立面 *</label>
        <input type="text" bind:value={form.facade} placeholder="如：东立面" required />
      </div>
      <div class="form-row">
        <label>房间号 *</label>
        <input type="text" bind:value={form.room} placeholder="如：501室" required />
      </div>
      <div class="form-row">
        <label>报修人 *</label>
        <input type="text" bind:value={form.residentName} required />
      </div>
      <div class="form-row">
        <label>联系电话</label>
        <input type="tel" bind:value={form.phone} />
      </div>
      <div class="form-row">
        <label>渗水位置描述</label>
        <input type="text" bind:value={form.locationNote} placeholder="如：5层窗台下沿" />
      </div>
    </div>

    <div class="form-row">
      <label>情况描述</label>
      <textarea bind:value={form.description} placeholder="渗水时间、位置、程度等"></textarea>
    </div>

    <div class="form-row">
      <label>现场照片（可多选）</label>
      <input type="file" accept="image/*" multiple on:change={onFiles} />
      {#if files.length}
        <div class="small muted">已选择 {files.length} 张：{files.map((f) => f.name).join('、')}</div>
      {/if}
    </div>

    {#if points.length}
      <div class="form-row">
        <label>同点位关联（可选）—— 若与已有渗水点位为同一处，选择后关联处理</label>
        <select bind:value={form.pointId}>
          <option value={null}>不关联，新建点位</option>
          {#each points as p}
            <option value={p.id}>{p.building} · {p.facade} · {p.note}（{p.tickets.length} 条工单）</option>
          {/each}
        </select>
      </div>
    {/if}

    {#if error}<div class="alert error">{error}</div>{/if}

    <div class="flex">
      <button class="btn-primary" type="submit" disabled={submitting}>
        {submitting ? '提交中…' : '提交报修'}
      </button>
      <a class="btn" href="#/">取消</a>
    </div>
  </form>
</div>
