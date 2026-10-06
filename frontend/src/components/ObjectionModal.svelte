<script>
  import { createEventDispatcher } from 'svelte';
  import { api, formDataWithFiles } from '../api.js';
  import PhotoUpload from './PhotoUpload.svelte';

  const dispatch = createEventDispatcher();
  export let reportId;

  let form = { reason: '' };
  let files = [];
  let err = '';
  let busy = false;

  async function submit() {
    err = '';
    busy = true;
    try {
      await api.addObjection(reportId, formDataWithFiles(form, 'photos', files));
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
    <h3>住户提出异议 <button class="close" on:click={() => dispatch('close')}>×</button></h3>
    {#if err}<div class="form-error">{err}</div>{/if}
    <form on:submit|preventDefault={submit}>
      <div class="row">
        <label>异议理由 *</label>
        <textarea bind:value={form.reason} placeholder="不认可复查结论的具体情况，如：复查后再次降雨仍出现水渍"></textarea>
      </div>
      <PhotoUpload on:change={(e) => (files = e.detail)} />
      <div class="callout">
        提交后报修单进入「异议处理中」，由物业继续处理并回复；物业可选择维持原结论，
        或重新安排处理（报修单回到待维修，重新施工并再次等待雨后复查）。
      </div>
      <div class="actions" style="justify-content:flex-end">
        <button type="button" class="btn ghost" on:click={() => dispatch('close')}>取消</button>
        <button class="btn danger" disabled={busy}>{busy ? '提交中…' : '提交异议'}</button>
      </div>
    </form>
  </div>
</div>
