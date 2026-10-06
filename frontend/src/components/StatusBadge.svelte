<script>
  import { STATUS } from '../api.js';
  export let status;
  $: meta = STATUS[status] || { label: status, cls: 'st-reported' };
</script>

<span class="badge {meta.cls}">{meta.label}</span>
