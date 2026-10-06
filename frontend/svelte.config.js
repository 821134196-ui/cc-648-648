import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

export default {
  preprocess: vitePreprocess(),
  // 图片灯箱、模态遮罩使用 div/img + click，演示项目中关闭对应键盘 a11y 提示
  onwarn(warning, handler) {
    if (['a11y-click-events-have-key-events', 'a11y-no-static-element-interactions']
      .includes(warning.code)) {
      return;
    }
    handler(warning);
  }
};
