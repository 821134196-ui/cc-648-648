<script>
  import { createEventDispatcher } from 'svelte';
  import { api } from '../api.js';

  const dispatch = createEventDispatcher();
  export let objectionId;

  let form = { outcome: 'REOPEN', replyNote: '', repliedBy: '' };
  let err = '';
  let busy = false;

  async function submit() {
    err = '';
    busy = true;
    try {
      await api.replyObjection(objectionId, form);
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
    <h3>物业回复异议 <button class="close" on:click={() => dispatch('close')}>×</button></h3>
    {#if err}<div class="form-error">{err}</div>{/if}
    <form on:submit|preventDefault={submit}>
      <div class="form-grid two">
        <div class="row">
          <label>处理结果 *</label>
          <select bind:value={form.outcome}>
            <option value="MAINTAIN">维持原结论（仍为已验证修复）</option>
            <option value="REOPEN">重新安排处理（退回待维修）</option>
          </select>
        </div>
        <div class="row">
          <label>回复人</label>
          <input type="text" bind:value={form.repliedBy} placeholder="如 物业李经理" />
        </div>
      </div>
      <div class="row">
        <label>回复内容 *</label>
        <textarea bind:value={form.replyNote} placeholder="核查情况与后续安排"></textarea>
      </div>
      <div class="actions" style="justify-content:flex-end">
        <button type="button" class="btn ghost" on:click={() => dispatch('close')}>取消</button>
        <button class="btn" disabled={busy}>{busy ? '提交中…' : '提交回复'}</button>
      </div>
    </form>
  </div>
</div>
