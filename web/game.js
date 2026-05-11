// ── Square Garden - Interactive Demo ──

(() => {
  'use strict';

  const GRID_SIZE = 7;
  let board = [];
  let movesRemaining = 12;
  let goalsCompleted = [false, false, false];
  let completedCells = new Set();
  let audioCtx = null;
  let demoRunning = false;

  // Starting board (7x7). R=Red, B=Blue, Y=Yellow, G=Green
  const DEMO_START = [
    ['Y','G','G','Y','R','R','Y'],
    ['G','Y','R','G','B','Y','R'],
    ['R','B','G','B','Y','R','Y'],
    ['B','R','R','G','Y','B','G'],
    ['Y','Y','G','Y','B','R','G'],
    ['G','R','R','B','R','G','G'],
    ['R','B','Y','Y','G','G','Y'],
  ];

  // Scripted move sequence with slide direction
  const DEMO_MOVES = [
    { from: [2, 1], to: [2, 2], thinkTime: 2000 },
    { from: [1, 4], to: [2, 4], thinkTime: 1500, completesGoal: 0 },
    { from: [3, 1], to: [4, 1], thinkTime: 2200 },
    { from: [6, 0], to: [6, 1], thinkTime: 1800 },
    { from: [5, 2], to: [6, 2], thinkTime: 2000, completesGoal: 2 },
    { from: [6, 6], to: [5, 6], thinkTime: 1500 },
    { from: [5, 6], to: [4, 6], thinkTime: 1800, completesGoal: 1 },
  ];

  const GOALS = [
    { name: 'Blue Line (3)', cells: [[2,2],[2,3],[2,4]], color: 'B' },
    { name: 'Green Square',  cells: [[5,5],[5,6],[6,5],[6,6]], color: 'G' },
    { name: 'Red L-Shape',   cells: [[4,1],[5,1],[6,1],[6,2]], color: 'R' },
  ];

  // ── DOM refs ──
  const boardEl = document.getElementById('game-board');
  const handEl = document.getElementById('hand-cursor');
  const movesEl = document.getElementById('moves-display');
  const winOverlay = document.getElementById('win-overlay');
  const confettiCanvas = document.getElementById('confetti-canvas');
  const confettiCtx = confettiCanvas.getContext('2d');

  // ── Audio (Web Audio API) ──
  function getAudioCtx() {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    return audioCtx;
  }

  function playTone(freq, duration, type = 'sine', volume = 0.3) {
    try {
      const ctx = getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq, ctx.currentTime);
      gain.gain.setValueAtTime(volume, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + duration);
    } catch (e) {}
  }

  function playTapSound() { playTone(880, 0.06, 'sine', 0.2); }

  function playSwapSound() {
    try {
      const ctx = getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(400, ctx.currentTime);
      osc.frequency.linearRampToValueAtTime(800, ctx.currentTime + 0.1);
      gain.gain.setValueAtTime(0.25, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.15);
    } catch (e) {}
  }

  function playGoalSound() {
    [523, 659, 784].forEach((f, i) => {
      setTimeout(() => playTone(f, 0.3, 'sine', 0.3), i * 100);
    });
  }

  function playWinFanfare() {
    [262, 330, 392, 523].forEach((f, i) => {
      setTimeout(() => playTone(f, 2.0, 'sine', 0.2), i * 80);
    });
    [523, 659, 784, 1047].forEach((f, i) => {
      setTimeout(() => playTone(f, 0.5, 'triangle', 0.15), 400 + i * 150);
    });
  }

  function playApplause() {
    try {
      const audio = new Audio('clapping.mp3');
      audio.volume = 0.7;
      audio.play();
    } catch (e) {}
  }

  // ── Board Rendering ──
  function deepCopy(b) { return b.map(row => [...row]); }

  function renderBoard() {
    boardEl.innerHTML = '';
    for (let r = 0; r < GRID_SIZE; r++) {
      for (let c = 0; c < GRID_SIZE; c++) {
        const tile = document.createElement('div');
        tile.className = `tile color-${board[r][c]}`;
        tile.dataset.row = r;
        tile.dataset.col = c;
        tile.id = `tile-${r}-${c}`;
        if (completedCells.has(`${r},${c}`)) tile.classList.add('completed-cell');
        boardEl.appendChild(tile);
      }
    }
  }

  function getTileRect(r, c) {
    const tile = document.getElementById(`tile-${r}-${c}`);
    if (!tile) return null;
    const wrapperRect = boardEl.parentElement.getBoundingClientRect();
    const tileRect = tile.getBoundingClientRect();
    return {
      x: tileRect.left - wrapperRect.left + tileRect.width / 2,
      y: tileRect.top - wrapperRect.top + tileRect.height / 2,
      w: tileRect.width,
      h: tileRect.height,
    };
  }

  // ── Animation Helpers ──
  function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

  function animateHandTo(x, y, duration = 600) {
    return new Promise(resolve => {
      handEl.style.transition = `left ${duration}ms cubic-bezier(0.4,0,0.2,1), top ${duration}ms cubic-bezier(0.4,0,0.2,1)`;
      handEl.style.left = x + 'px';
      handEl.style.top = y + 'px';
      setTimeout(resolve, duration);
    });
  }

  // Slide swap: hand grabs source tile, drags it to target, tiles swap
  async function slideSwap(r1, c1, r2, c2) {
    const srcTile = document.getElementById(`tile-${r1}-${c1}`);
    const tgtTile = document.getElementById(`tile-${r2}-${c2}`);
    if (!srcTile || !tgtTile) return;

    const srcRect = srcTile.getBoundingClientRect();
    const tgtRect = tgtTile.getBoundingClientRect();
    const dx = tgtRect.left - srcRect.left;
    const dy = tgtRect.top - srcRect.top;

    // Grab: press down on source
    playTapSound();
    srcTile.classList.add('dragging');
    handEl.classList.add('pressing');
    await sleep(150);

    // Slide: move source tile toward target, hand follows
    const tgtPos = getTileRect(r2, c2);
    srcTile.style.transition = 'transform 0.35s cubic-bezier(0.4,0,0.2,1)';
    srcTile.style.transform = `translate(${dx}px, ${dy}px) scale(1.08)`;
    srcTile.style.zIndex = '20';

    // Target tile slides opposite direction
    tgtTile.style.transition = 'transform 0.35s cubic-bezier(0.4,0,0.2,1)';
    tgtTile.style.transform = `translate(${-dx}px, ${-dy}px)`;

    // Hand slides along with the tile
    animateHandTo(tgtPos.x, tgtPos.y, 350);

    playSwapSound();
    await sleep(380);

    // Release
    srcTile.classList.remove('dragging');
    handEl.classList.remove('pressing');

    // Commit the swap in data
    const tmp = board[r1][c1];
    board[r1][c1] = board[r2][c2];
    board[r2][c2] = tmp;
    renderBoard();
  }

  function markGoalCells(goalIdx) {
    const goal = GOALS[goalIdx];
    goal.cells.forEach(([r, c]) => {
      completedCells.add(`${r},${c}`);
      const tile = document.getElementById(`tile-${r}-${c}`);
      if (tile) { tile.classList.add('completed-cell'); tile.classList.add('bounce'); }
    });
    const goalEl = document.getElementById(`goal-${goalIdx}`);
    if (goalEl) goalEl.classList.add('completed');
  }

  // ── Confetti ──
  let confettiParticles = [];
  let confettiRunning = false;
  const CONFETTI_COLORS = ['#E53935','#FFB300','#43A047','#1E88E5','#E91E63','#7C4DFF','#F57C00','#00BCD4'];

  function startConfetti(count = 200, duration = 6000) {
    const W = confettiCanvas.width = confettiCanvas.parentElement.offsetWidth;
    const H = confettiCanvas.height = confettiCanvas.parentElement.offsetHeight;
    confettiParticles = [];
    for (let i = 0; i < count; i++) {
      confettiParticles.push({
        x: Math.random() * W,
        y: -20 - Math.random() * H * 0.5,
        w: 6 + Math.random() * 10,
        h: 8 + Math.random() * 12,
        color: CONFETTI_COLORS[Math.floor(Math.random() * CONFETTI_COLORS.length)],
        speed: 0.5 + Math.random(),
        sway: 0.02 + Math.random() * 0.06,
        phase: Math.random() * Math.PI * 2,
        rot: Math.random() * 360,
        rotSpeed: 100 + Math.random() * 400,
        shape: Math.floor(Math.random() * 3),
      });
    }
    confettiRunning = true;
    const start = performance.now();

    function frame(t) {
      if (!confettiRunning) return;
      const elapsed = t - start;
      const progress = Math.min(elapsed / duration, 1);
      confettiCtx.clearRect(0, 0, W, H);

      confettiParticles.forEach(p => {
        const py = p.y + (H + 60) * progress * p.speed;
        const px = p.x + Math.sin(progress * 8 + p.phase) * p.sway * W;
        const rot = (p.rot + p.rotSpeed * progress) * Math.PI / 180;
        const alpha = progress > 0.85 ? 1 - (progress - 0.85) / 0.15 : 1;

        confettiCtx.save();
        confettiCtx.globalAlpha = alpha;
        confettiCtx.translate(px, py);
        confettiCtx.rotate(rot);
        confettiCtx.fillStyle = p.color;

        if (p.shape === 0) {
          confettiCtx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
        } else if (p.shape === 1) {
          confettiCtx.beginPath();
          confettiCtx.arc(0, 0, p.w / 2, 0, Math.PI * 2);
          confettiCtx.fill();
        } else {
          confettiCtx.beginPath();
          confettiCtx.moveTo(0, -p.h / 2);
          confettiCtx.lineTo(p.w / 2, p.h / 2);
          confettiCtx.lineTo(-p.w / 2, p.h / 2);
          confettiCtx.closePath();
          confettiCtx.fill();
        }
        confettiCtx.restore();
      });

      if (progress < 1) requestAnimationFrame(frame);
      else { confettiCtx.clearRect(0, 0, W, H); confettiRunning = false; }
    }
    requestAnimationFrame(frame);
  }

  // ── Win Sequence ──
  async function showWin() {
    for (let r = 0; r < GRID_SIZE; r++) {
      for (let c = 0; c < GRID_SIZE; c++) {
        const tile = document.getElementById(`tile-${r}-${c}`);
        if (tile) setTimeout(() => tile.classList.add('wave-bounce'), (r + c) * 60);
      }
    }
    await sleep(600);

    playWinFanfare();
    setTimeout(playApplause, 300);
    startConfetti(250, 6000);
    winOverlay.classList.add('visible');
    document.getElementById('star-1').classList.add('show');
    document.getElementById('star-2').classList.add('show');
    document.getElementById('star-3').classList.add('show');

    await sleep(7000);

    winOverlay.style.transition = 'opacity 1s';
    winOverlay.classList.remove('visible');
    await sleep(1200);
    resetDemo();
  }

  // ── Demo Loop ──
  function resetDemo() {
    board = deepCopy(DEMO_START);
    movesRemaining = 12;
    goalsCompleted = [false, false, false];
    completedCells = new Set();
    movesEl.textContent = '12';
    handEl.classList.remove('visible', 'thinking', 'pressing');
    handEl.style.transition = 'none';
    handEl.style.left = '-40px';
    handEl.style.top = '50%';
    winOverlay.classList.remove('visible');
    winOverlay.style.transition = '';
    document.querySelectorAll('.goal-card').forEach(g => g.classList.remove('completed'));
    document.querySelectorAll('.win-star').forEach(s => s.classList.remove('show'));
    confettiRunning = false;
    confettiCtx.clearRect(0, 0, confettiCanvas.width, confettiCanvas.height);
    renderBoard();
    setTimeout(() => runDemo(), 1500);
  }

  async function runDemo() {
    if (demoRunning) return;
    demoRunning = true;

    await sleep(500);
    handEl.classList.add('visible');

    for (let i = 0; i < DEMO_MOVES.length; i++) {
      const move = DEMO_MOVES[i];
      const [fr, fc] = move.from;
      const [tr, tc] = move.to;

      // Move hand to source tile
      const srcPos = getTileRect(fr, fc);
      await animateHandTo(srcPos.x, srcPos.y, 700);

      // Thinking pause with wobble
      handEl.classList.add('thinking');
      await sleep(move.thinkTime);
      handEl.classList.remove('thinking');

      // Slide swap: grab, drag, release
      await slideSwap(fr, fc, tr, tc);
      movesRemaining--;
      movesEl.textContent = movesRemaining;

      // Goal completion check
      if (move.completesGoal !== undefined && !goalsCompleted[move.completesGoal]) {
        await sleep(300);
        goalsCompleted[move.completesGoal] = true;
        markGoalCells(move.completesGoal);
        playGoalSound();
        await sleep(600);
      }

      await sleep(400);
    }

    // Hide hand and trigger win
    handEl.classList.remove('visible');
    await sleep(500);
    await showWin();
    demoRunning = false;
  }

  // ── Init ──
  function init() {
    board = deepCopy(DEMO_START);
    renderBoard();

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !demoRunning) {
        observer.disconnect();
        document.addEventListener('click', () => getAudioCtx(), { once: true });
        document.addEventListener('touchstart', () => getAudioCtx(), { once: true });
        runDemo();
      }
    }, { threshold: 0.3 });
    observer.observe(boardEl);

    setTimeout(() => { if (!demoRunning) runDemo(); }, 2000);
  }

  init();
})();
