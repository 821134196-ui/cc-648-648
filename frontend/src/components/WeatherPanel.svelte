<script>
  import { createEventDispatcher } from 'svelte';
  import { api } from '../api.js';
  import { fmtDateTime } from '../api.js';

  export let weather;
  const dispatch = createEventDispatcher();

  let date = new Date().toISOString().slice(0, 10);
  let mm = 18;
  let note = '';
  let err = '';
  let adding = false;

  async function add() {
    err = '';
    adding = true;
    try {
      await api.addRain({ rainDate: date, mm: Number(mm), note });
      date = new Date().toISOString().slice(0, 10);
      mm = 18;
      note = '';
      dispatch('changed');
    } catch (e) {
      err = e.message;
    } finally {
      adding = false;
    }
  }

  async function remove(id) {
    await api.deleteRain(id);
    dispatch('changed');
  }
</script>

<div class="panel">
  <h2>
    🌦️ 本地模拟天气
    <span class="hint">
      有效降雨阈值 <b>{weather.thresholdMm}mm</b>，且降雨日期须严格晚于施工完成日（施工当天的雨不算）
    </span>
  </h2>

  <div class="weather-strip" style="margin-bottom:12px">
    {#each weather.rainRecords as r}
      <span class="rain-chip {r.mm >= weather.thresholdMm ? 'heavy' : ''}" title={r.note}>
        {r.mm >= weather.thresholdMm ? '🌧️' : '🌦️'} {r.rainDate} · {r.mm}mm
        {#if r.note}<span class="muted">（{r.note.length > 14 ? r.note.slice(0, 14) + '…' : r.note}）</span>{/if}
        <button title="删除该降雨记录" on:click={() => remove(r.id)}>×</button>
      </span>
    {:else}
      <span class="muted">暂无降雨记录</span>
    {/each}
  </div>

  <form class="weather-strip" on:submit|preventDefault={add}>
    <label style="font-size:12.5px;color:var(--ink-2)">模拟一场降雨：</label>
    <input type="date" bind:value={date} style="width:160px" />
    <input type="number" bind:value={mm} min="1" step="0.5" style="width:110px" placeholder="mm" />
    <input type="text" bind:value={note} placeholder="备注，如：大雨/暴雨" style="width:200px" />
    <button class="btn sm" disabled={adding}>{adding ? '登记中…' : '登记降雨'}</button>
    {#if err}<span class="form-error" style="margin:0;padding:4px 10px">{err}</span>{/if}
  </form>
</div>
