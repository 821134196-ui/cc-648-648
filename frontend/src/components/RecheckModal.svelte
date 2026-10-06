<script>
  import { createEventDispatcher } from 'svelte';
  import { api, formDataWithFiles } from '../api.js';
  import PhotoUpload from './PhotoUpload.svelte';

  const dispatch = createEventDispatcher();
  export let reportId;
  export let eligible;
  export let rainMessage;

  let form = { inspectorName: '', result: 'DRY', conclusion: '' };
  let files = [];
  let err = '';
  let busy = false;

  async function submit() {
    err = '';
    busy = true;
    try {
      await api.addRecheck(reportId, formDataWithFiles(form, 'photos', files));
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
    <h3>复查人员雨后复查 <button class="close" on:click={() => dispatch('close')}>×</button></h3>

    {#if !eligible}
      <div class="form-error">
        ⛔ 当前不满足雨后复查条件，不能提交“未见渗水”的验证结论：<br />{rainMessage}
      </div>
      <div class="callout">
        请等待有效降雨。可在页面顶部「本地模拟天气」登记一场晚于施工日、达到阈值的降雨后再复查。
        施工当天的降雨不算雨后。
      </div>
      <div class="actions" style="justify-content:flex-end">
        <button type="button" class="btn ghost" on:click={() => dispatch('close')}>知道了</button>
      </div>
    {:else}
      {#if err}<div class="form-error">{err}</div>{/if}
      <form on:submit|preventDefault={submit}>
        <div class="form-grid two">
          <div class="row">
            <label>复查人员 *</label>
            <input type="text" bind:value={form.inspectorName} placeholder="如 孙质检" />
          </div>
          <div class="row">
            <label>复查结论 *</label>
            <select bind:value={form.result}>
              <option value="DRY">未见渗水（验证修复）</option>
              <option value="LEAKING">仍有渗水（退回重新维修）</option>
            </select>
          </div>
        </div>
        <div class="row">
          <label>复查说明</label>
          <textarea bind:value={form.conclusion} placeholder="雨后回原址检查的具体情况"></textarea>
        </div>
        <PhotoUpload on:change={(e) => (files = e.detail)} />
        <div class="callout">
          本次复查将记录所依据的有效降雨日期与降雨量。结论为“仍有渗水”时，报修单退回待维修，
          重新施工后需再次等待有效降雨。
        </div>
        <div class="actions" style="justify-content:flex-end">
          <button type="button" class="btn ghost" on:click={() => dispatch('close')}>取消</button>
          <button class="btn" disabled={busy}>{busy ? '提交中…' : '提交复查结果'}</button>
        </div>
      </form>
    {/if}
  </div>
</div>
