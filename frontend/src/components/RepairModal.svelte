<script>
  import { createEventDispatcher } from 'svelte';
  import { api, formDataWithFiles } from '../api.js';
  import PhotoUpload from './PhotoUpload.svelte';

  const dispatch = createEventDispatcher();
  export let reportId;

  const now = () => {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  };

  let form = { workerName: '', summary: '', happenedAt: now() };
  let files = [];
  let err = '';
  let busy = false;

  async function submit() {
    err = '';
    busy = true;
    try {
      await api.addRepair(reportId, formDataWithFiles(form, 'photos', files));
      dispatch('done');
    } catch (e) {
      err = e.message;
    } finally {
      busy = false;
    }
  }
</script>

<div class="modal-mask" on:click|self={() => dispatch('close')}>
  <div class="modal">
    <h3>维修员记录施工 <button class="close" on:click={() => dispatch('close')}>×</button></h3>
    {#if err}<div class="form-error">{err}</div>{/if}
    <form on:submit|preventDefault={submit}>
      <div class="form-grid two">
        <div class="row">
          <label>维修员 *</label>
          <input type="text" bind:value={form.workerName} placeholder="如 赵师傅" />
        </div>
        <div class="row">
          <label>施工完成时间 *</label>
          <input type="datetime-local" bind:value={form.happenedAt} />
        </div>
      </div>
      <div class="row">
        <label>施工内容（工艺/材料/处理范围）</label>
        <textarea bind:value={form.summary} placeholder="如：外墙裂缝注胶封堵，窗上口重做防水涂层"></textarea>
      </div>
      <PhotoUpload on:change={(e) => (files = e.detail)} />
      <div class="callout warn">
        保存后该点位关联的全部住户报修单进入「待复查」，必须等待一场晚于施工日、
        达到阈值的有效降雨，复查人员才能回原址验证。
      </div>
      <div class="actions" style="justify-content:flex-end">
        <button type="button" class="btn ghost" on:click={() => dispatch('close')}>取消</button>
        <button class="btn" disabled={busy}>{busy ? '保存中…' : '保存施工记录'}</button>
      </div>
    </form>
  </div>
</div>
