<script>
  import { createEventDispatcher } from 'svelte';
  import { api, formDataWithFiles } from '../api.js';
  import PhotoUpload from './PhotoUpload.svelte';

  const dispatch = createEventDispatcher();

  const form = {
    building: '', facade: '', location: '', room: '',
    residentName: '', contact: '', description: ''
  };
  let files = [];
  let err = '';
  let busy = false;

  async function submit() {
    err = '';
    busy = true;
    try {
      const fd = formDataWithFiles(form, 'photos', files);
      const detail = await api.submitReport(fd);
      dispatch('created', detail.report.id);
    } catch (e) {
      err = e.message;
    } finally {
      busy = false;
    }
  }
</script>

<div class="modal-mask" on:click|self={() => dispatch('close')}>
  <div class="modal">
    <h3>住户提交报修 <button class="close" on:click={() => dispatch('close')}>×</button></h3>

    {#if err}<div class="form-error">{err}</div>{/if}

    <form on:submit|preventDefault={submit}>
      <div class="form-grid">
        <div class="row">
          <label>楼栋 *</label>
          <input type="text" bind:value={form.building} placeholder="如 3栋" />
        </div>
        <div class="row">
          <label>立面 *</label>
          <select bind:value={form.facade}>
            <option value="">请选择</option>
            <option>东立面</option><option>南立面</option>
            <option>西立面</option><option>北立面</option>
          </select>
        </div>
        <div class="row">
          <label>渗水位置 *</label>
          <input type="text" bind:value={form.location} placeholder="如 主卧窗上角" />
        </div>
      </div>
      <div class="form-grid">
        <div class="row">
          <label>房间 *</label>
          <input type="text" bind:value={form.room} placeholder="如 2单元1202" />
        </div>
        <div class="row">
          <label>住户姓名 *</label>
          <input type="text" bind:value={form.residentName} />
        </div>
        <div class="row">
          <label>联系方式</label>
          <input type="text" bind:value={form.contact} />
        </div>
      </div>
      <div class="row">
        <label>渗水情况描述</label>
        <textarea bind:value={form.description} placeholder="何时发现、水量、影响范围等"></textarea>
      </div>

      <PhotoUpload on:change={(e) => (files = e.detail)} />

      <div class="callout">
        提示：楼栋 + 立面 + 位置相同的报修会自动关联为同一渗水点位，联合维修、共用雨后复查条件，
        但各住户的报修与复查记录独立保留。同一户在该位置再次报修会记为<b>原问题复发</b>。
      </div>

      <div class="actions" style="justify-content:flex-end">
        <button type="button" class="btn ghost" on:click={() => dispatch('close')}>取消</button>
        <button class="btn" disabled={busy}>{busy ? '提交中…' : '提交报修'}</button>
      </div>
    </form>
  </div>
</div>
