<script>
  import { onMount } from 'svelte';
  import TicketList from './pages/TicketList.svelte';
  import TicketNew from './pages/TicketNew.svelte';
  import TicketDetail from './pages/TicketDetail.svelte';
  import Points from './pages/Points.svelte';
  import Weather from './pages/Weather.svelte';

  let hash = location.hash || '#/';

  onMount(() => {
    const onChange = () => (hash = location.hash || '#/');
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  });

  $: parts = hash.replace(/^#/, '').split('/').filter(Boolean);
  $: page = parts[0] || 'home';
  $: param = parts[1];
</script>

<header class="topbar">
  <div class="topbar-inner">
    <a class="brand" href="#/">
      <span class="brand-icon">🏢</span>
      <span>外墙渗水季节复查系统</span>
    </a>
    <nav>
      <a href="#/" class:active={page === 'home'}>工单</a>
      <a href="#/points" class:active={page === 'points'}>渗水点位</a>
      <a href="#/weather" class:active={page === 'weather'}>天气模拟</a>
      <a class="btn-primary nav-cta" href="#/new">＋ 新建报修</a>
    </nav>
  </div>
</header>

<main class="container">
  {#if page === 'home'}
    <TicketList />
  {:else if page === 'new'}
    <TicketNew />
  {:else if page === 'ticket' && param}
    <TicketDetail id={param} />
  {:else if page === 'points'}
    <Points />
  {:else if page === 'weather'}
    <Weather />
  {:else}
    <div class="card"><p>页面不存在，<a href="#/">返回工单列表</a></p></div>
  {/if}
</main>

<footer class="footer">物业楼栋外墙渗水季节复查系统 · 本地演示环境</footer>
