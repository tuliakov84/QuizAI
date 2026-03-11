// timer-worker.js
let timer = null;
let startTime = 0;
let duration = 0;

self.onmessage = function(e) {
  if (e.data.action === 'start') {
    duration = e.data.duration;
    startTime = Date.now();

    clearInterval(timer);
    timer = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const remaining = Math.max(0, duration - elapsed);
      self.postMessage({
        type: 'tick',
        remainingMs: remaining
      });

      if (remaining <= 0) {
        clearInterval(timer);
        self.postMessage({ type: 'timeout' });
      }
    }, 50);
  } else if (e.data === 'stop') {
    clearInterval(timer);
  }
};