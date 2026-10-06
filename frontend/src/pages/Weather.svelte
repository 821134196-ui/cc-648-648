<script>
  import { onMount } from 'svelte';
  import { api } from '../api.js';

  let weather = null;
  let reminders = [];
  let error = '';
  let msg = '';
  let form = { date: '', rainfallMm: '', note: '' };

  async function load() {
    error = '';
    try {
      [weather, reminders] = await Promise.all([api.weather(), api.reminders()]);
    } catch (e) {
      error = e.message;
    }
  }

  onMount(load);

  async function submit() {
    error = '';
    msg = '';
    try {
      await api.addRain({
        date: form.date,
        rainfallMm: Number(form.rainfallMm),
        note: form.note,
      });
      msg = `已录入 ${form.date} 降雨 ${form.rainfallMm}mm，模拟时钟已推进`;
      form = { date: '', rainfallMm: '', note: '' };
      await load();
    } catch (e) {
      error = e.message;
    }
  }
</script>

<div class="page-title"><h1>天气模拟（本地）</h1></div>

{#if error}<div class="alert error">{error}</div>{/if}
{#if msg}<div class="alert success">{msg}</div>{/if}

{#if weather}
  <div class="card">
    <div class="meta-grid">
      <div><div class="k">模拟当前日期</div><div class="v">{weather.currentDate}</div></div>
      <div><div class="k">有效降雨阈值</div><div class="v">≥ {weather.thresholdMm} mm</div></div>
      <div><div class="k">待复查工单</div><div class="v">{reminders.length} 个</div></div>
    </div>
    <p class="small muted mt8">
      规则：工单完工后须发生一场“有效降雨”（雨量 ≥ 阈值，且降雨日晚于完工日），复查人员才可在雨后回到原位置检查；
      完工当天或无有效降雨时，不能标记验证通过。
    </p>
  </div>

  <div class="card">
    <h2>录入模拟降雨</h2>
    <form class="form-grid" on:submit|preventDefault={submit}>
      <div class="form-row">
        <label>日期 *</label>
        <input type="date" bind:value={form.date} required />
      </div>
      <div class="form-row">
        <label>降雨量（mm）*</label>
        <input type="number" min="0" step="0.1" bind:value={form.rainfallMm} required />
      </div>
      <div class="form-row">
        <label>备注</label>
        <input type="text" bind:value={form.note} placeholder="如：台风外围强降雨" />
      </div>
      <div class="form-row" style="align-self: end;">
        <button class="btn-primary" type="submit">录入降雨</button>
      </div>
    </form>
  </div>

  <div class="card">
    <h2>降雨记录</h2>
    <table class="list">
      <thead><tr><th>日期</th><th>降雨量</th><th>是否有效</th><th>备注</th></tr></thead>
      <tbody>
        {#each weather.events as e}
          <tr style="cursor: default;">
            <td>{e.date}</td>
            <td>{e.rainfallMm} mm</td>
            <td>
              {#if e.effective}
                <span class="badge badge-pass">有效降雨</span>
              {:else}
                <span class="badge st-reported">未达阈值</span>
              {/if}
            </td>
            <td class="muted small">{e.note || '—'}</td>
          </tr>
        {:else}
          <tr style="cursor: default;"><td colspan="4" class="muted">暂无降雨记录</td></tr>
        {/each}
      </tbody>
    </table>
  </div>

  <div class="card">
    <h2>待复查提醒</h2>
    {#if reminders.length}
      <table class="list">
        <thead><tr><th>工单</th><th>位置</th><th>报修人</th><th>完工日期</th><th>复查条件</th></tr></thead>
        <tbody>
          {#each reminders as r}
            <tr on:click={() => (location.hash = `#/ticket/${r.ticketId}`)}>
              <td>#{r.ticketId}</td>
              <td>{r.building} · {r.facade} · {r.room}</td>
              <td>{r.residentName}</td>
              <td>{r.completedAt || '—'}</td>
              <td>
                {#if r.eligible}
                  <span class="badge badge-pass">可复查</span>
                {:else}
                  <span class="badge st-pending">待有效降雨</span>
                {/if}
                <div class="small muted">{r.reason}</div>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {:else}
      <p class="muted">当前没有待复查的工单</p>
    {/if}
  </div>
{/if}
