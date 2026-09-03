// =========================================================
// INNOVA 2KD MINIMALIST CO-PILOT LAUNCHER - SIMULATOR LOGIC
// =========================================================

document.addEventListener('DOMContentLoaded', () => {
  // State
  let speed = 85;
  let speedLimit = 100;
  let odoTotal = 142350;
  let tripDistance = 45.2;
  let activeSlotToRebind = null;

  // Shortcuts data
  const shortcuts = [
    { label: 'GOOGLE MAPS', pkg: 'com.google.android.apps.maps', icon: 'M12,2L4.5,20.29l0.71,0.71L12,18l6.79,3 0.71,-0.71z' },
    { label: 'SPOTIFY / MUSIK', pkg: 'com.spotify.music', icon: 'M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z' },
    { label: 'RADIO FM', pkg: 'com.android.fmradio', icon: 'M3.24,6.15C2.51,6.43 2,7.17 2,8v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2H8.3l8.26,-3.34L15.88,1 3.24,6.15zM7,20c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zm13,-8h-2v-2h-2v2H4V8h16v4z' }
  ];

  // All Apps list
  const allApps = [
    { label: 'Google Maps', pkg: 'com.google.android.apps.maps', category: 'Navigasi' },
    { label: 'Waze GPS', pkg: 'com.waze', category: 'Navigasi' },
    { label: 'Spotify', pkg: 'com.spotify.music', category: 'Audio' },
    { label: 'YouTube Music', pkg: 'com.google.android.apps.youtube.music', category: 'Audio' },
    { label: 'Radio FM', pkg: 'com.android.fmradio', category: 'Media' },
    { label: 'Bluetooth Audio', pkg: 'com.android.bluetooth', category: 'Telepon' },
    { label: 'USB File Manager', pkg: 'com.android.documentsui', category: 'Utilitas' },
    { label: 'Pengaturan Mobil', pkg: 'com.car.settings', category: 'Sistem' }
  ];

  // Fuse Data
  const engineFuses = [
    { code: 'EFI', amp: '25A', desc: 'Komputer ECU 2KD & Common Rail Supply Pump' },
    { code: 'GLOW', amp: '80A', desc: 'Busi Pijar Pemanas Mesin Diesel (Glow Plug)' },
    { code: 'ALT', amp: '140A', desc: 'Alternator Utama Pengisian Aki' },
    { code: 'HORN', amp: '10A', desc: 'Klakson Mobil' },
    { code: 'HEAD HI', amp: '20A', desc: 'Lampu Jauh (High Beam)' },
    { code: 'HEAD LO', amp: '15A', desc: 'Lampu Dekat (Low Beam)' },
    { code: 'AM2', amp: '30A', desc: 'Sistem Kunci Kontak Utama Starter' },
    { code: 'COND FAN', amp: '30A', desc: 'Ekstra Fan Kondensor AC' },
    { code: 'FOG', amp: '15A', desc: 'Lampu Kabut (Foglamp)' }
  ];

  const cabinFuses = [
    { code: 'CIG', amp: '15A', desc: 'Soket Lighter 12V / Colokan Charger HP' },
    { code: 'ACC', amp: '7.5A', desc: 'Arus Kontak Headunit & Spion Elektrik' },
    { code: 'POWER', amp: '30A', desc: 'Motor Power Window Kaca Pintu' },
    { code: 'WIPER', amp: '20A', desc: 'Wiper Kaca Depan & Belakang' },
    { code: 'GAUGE', amp: '10A', desc: 'Panel Spidometer & Indikator Dashboard' },
    { code: 'ECU-IG', amp: '10A', desc: 'Sistem Sensor ABS & Modul Airbag' },
    { code: 'STOP', amp: '10A', desc: 'Lampu Rem Belakang' },
    { code: 'DOME', amp: '10A', desc: 'Lampu Plafon Kabin & Jam Tengah' },
    { code: 'TAIL', amp: '10A', desc: 'Lampu Senja/Kota & Lampu Plat Nomor' },
    { code: 'OBD', amp: '7.5A', desc: 'Soket Diagnostik DLC3 Bawah Setir' }
  ];

  // DOM Elements
  const speedValue = document.getElementById('speedValue');
  const speedLimitBadge = document.getElementById('speedLimitBadge');
  const speedLimitText = document.getElementById('speedLimitText');
  const speedSlider = document.getElementById('speedSlider');
  const clockDisplay = document.getElementById('clockDisplay');

  // Web Audio Chime for Speed Warning
  let audioCtx = null;
  function playWarningChime() {
    try {
      if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, audioCtx.currentTime); // A5 note
      osc.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.3);
      gain.gain.setValueAtTime(0.15, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.3);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.3);
    } catch (e) {}
  }

  // Speedometer Update Logic
  let wasOverLimit = false;
  function updateSpeed(newSpeed) {
    speed = newSpeed;
    speedValue.textContent = speed;

    if (speed > speedLimit) {
      speedValue.classList.add('danger');
      speedLimitBadge.classList.add('danger');
      if (!wasOverLimit) {
        playWarningChime();
        wasOverLimit = true;
      }
    } else {
      speedValue.classList.remove('danger');
      speedLimitBadge.classList.remove('danger');
      wasOverLimit = false;
    }
  }

  speedSlider.addEventListener('input', (e) => {
    updateSpeed(parseInt(e.target.value));
  });

  // Clock Update
  function updateClock() {
    const now = new Date();
    const h = String(now.getHours()).padStart(2, '0');
    const m = String(now.getMinutes()).padStart(2, '0');
    clockDisplay.textContent = `${h}:${m}`;
  }
  updateClock();
  setInterval(updateClock, 10000);

  // Tab Navigation (Center Card)
  const tabBtnService = document.getElementById('tabBtnService');
  const tabBtnTrip = document.getElementById('tabBtnTrip');
  const tabBtnMedia = document.getElementById('tabBtnMedia');
  const tabContentService = document.getElementById('tabContentService');
  const tabContentTrip = document.getElementById('tabContentTrip');
  const tabContentMedia = document.getElementById('tabContentMedia');

  function switchTab(btn, content) {
    [tabBtnService, tabBtnTrip, tabBtnMedia].forEach(b => b.classList.remove('active'));
    [tabContentService, tabContentTrip, tabContentMedia].forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    content.classList.add('active');
  }

  tabBtnService.addEventListener('click', () => switchTab(tabBtnService, tabContentService));
  tabBtnTrip.addEventListener('click', () => switchTab(tabBtnTrip, tabContentTrip));
  tabBtnMedia.addEventListener('click', () => switchTab(tabBtnMedia, tabContentMedia));

  // Reset Service Button
  document.getElementById('btnResetService').addEventListener('click', () => {
    if (confirm('Konfirmasi: Reset siklus Oli Mesin & Filter Solar setelah servis bengkel?')) {
      document.getElementById('oilCountdown').textContent = '5.000 KM lagi';
      document.getElementById('filterCountdown').textContent = '10.000 KM lagi';
      alert('Siklus Servis 2KD Berhasil Direset!');
    }
  });

  // Media Play/Pause
  let isPlaying = false;
  const btnPlayPause = document.getElementById('btnPlayPause');
  btnPlayPause.addEventListener('click', () => {
    isPlaying = !isPlaying;
    btnPlayPause.textContent = isPlaying ? '⏸' : '▶';
  });

  // Screen Off Overlay
  const btnScreenOff = document.getElementById('btnScreenOff');
  const screenOffOverlay = document.getElementById('screenOffOverlay');

  btnScreenOff.addEventListener('click', () => {
    screenOffOverlay.classList.add('active');
  });

  screenOffOverlay.addEventListener('click', () => {
    screenOffOverlay.classList.remove('active');
  });

  // Shortcuts Bindings & Long-press rebind
  function updateShortcutUI() {
    document.getElementById('scLabel1').textContent = shortcuts[0].label;
    document.getElementById('scLabel2').textContent = shortcuts[1].label;
    document.getElementById('scLabel3').textContent = shortcuts[2].label;
  }

  [1, 2, 3].forEach(idx => {
    const btn = document.getElementById(`scBtn${idx}`);
    let pressTimer;

    btn.addEventListener('mousedown', () => {
      pressTimer = setTimeout(() => {
        // Long press: Rebind shortcut
        activeSlotToRebind = idx - 1;
        openModal(modalApps);
      }, 700);
    });

    btn.addEventListener('mouseup', () => clearTimeout(pressTimer));
    btn.addEventListener('mouseleave', () => clearTimeout(pressTimer));

    btn.addEventListener('click', () => {
      alert(`Membuka Aplikasi: ${shortcuts[idx - 1].label}`);
    });
  });

  // All Apps Grid Populate
  const appsGridContainer = document.getElementById('appsGridContainer');
  allApps.forEach(app => {
    const el = document.createElement('div');
    el.className = 'app-grid-item';
    el.innerHTML = `
      <div class="app-icon-box">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M4,8h4V4H4v4zm6,12h4v-4h-4v4zm-6,0h4v-4H4v4zm0,-6h4v-4H4v4zm6,0h4v-4h-4v4zm6,-10v4h4V4h-4zm-6,4h4V4h-4v4zm6,6h4v-4h-4v4zm0,6h4v-4h-4v4z"/>
        </svg>
      </div>
      <span class="app-grid-label">${app.label}</span>
    `;
    el.addEventListener('click', () => {
      if (activeSlotToRebind !== null) {
        shortcuts[activeSlotToRebind].label = app.label.toUpperCase();
        updateShortcutUI();
        alert(`Tombol Shortcut ${activeSlotToRebind + 1} berhasil diubah menjadi: ${app.label}`);
        activeSlotToRebind = null;
        closeModal(modalApps);
      } else {
        alert(`Membuka: ${app.label}`);
        closeModal(modalApps);
      }
    });
    appsGridContainer.appendChild(el);
  });

  // Modals Handling
  const modalApps = document.getElementById('modalApps');
  const modalAudio = document.getElementById('modalAudio');
  const modalFuse = document.getElementById('modalFuse');
  const modalSettings = document.getElementById('modalSettings');

  function openModal(modal) { modal.classList.add('active'); }
  function closeModal(modal) { modal.classList.remove('active'); activeSlotToRebind = null; }

  document.getElementById('btnAllApps').addEventListener('click', () => openModal(modalApps));
  document.getElementById('btnCloseApps').addEventListener('click', () => closeModal(modalApps));

  document.getElementById('btnAudioModal').addEventListener('click', () => openModal(modalAudio));
  document.getElementById('btnCloseAudio').addEventListener('click', () => closeModal(modalAudio));

  document.getElementById('btnFuseModal').addEventListener('click', () => {
    renderFuses(engineFuses);
    openModal(modalFuse);
  });
  document.getElementById('btnCloseFuse').addEventListener('click', () => closeModal(modalFuse));

  document.getElementById('btnSettingsModal').addEventListener('click', () => openModal(modalSettings));
  document.getElementById('btnCloseSettings').addEventListener('click', () => closeModal(modalSettings));

  // Audio DSP Presets
  const presetButtons = document.querySelectorAll('.btn-preset');
  const eqSliders = document.querySelectorAll('.v-slider');
  const presetValues = {
    btnPresetClarity: [2, 6, 3, 0, 1, 5, 6, 4, 2, 3],
    btnPresetBass: [6, 8, 4, -2, -1, 1, 2, 2, 4, 5],
    btnPresetVocal: [-2, 0, 1, 3, 6, 5, 4, 2, 1, 0],
    btnPresetFlat: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
  };

  presetButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      presetButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const vals = presetValues[btn.id] || [0,0,0,0,0,0,0,0,0,0];
      eqSliders.forEach((s, idx) => {
        s.value = vals[idx];
        s.previousElementSibling.textContent = (vals[idx] > 0 ? '+' : '') + vals[idx];
      });
    });
  });

  // Sound Stage
  const stageButtons = document.querySelectorAll('.btn-stage');
  stageButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      stageButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });

  // Fuse Table Rendering
  const fuseTableBody = document.getElementById('fuseTableBody');
  const fuseTabEngine = document.getElementById('fuseTabEngine');
  const fuseTabCabin = document.getElementById('fuseTabCabin');

  function renderFuses(list) {
    fuseTableBody.innerHTML = '';
    list.forEach(item => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${item.code}</td>
        <td>${item.amp}</td>
        <td>${item.desc}</td>
      `;
      fuseTableBody.appendChild(tr);
    });
  }

  fuseTabEngine.addEventListener('click', () => {
    fuseTabEngine.classList.add('active');
    fuseTabCabin.classList.remove('active');
    renderFuses(engineFuses);
  });

  fuseTabCabin.addEventListener('click', () => {
    fuseTabCabin.classList.add('active');
    fuseTabEngine.classList.remove('active');
    renderFuses(cabinFuses);
  });

  // Settings & Themes
  const themeButtons = document.querySelectorAll('.theme-btn');
  themeButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      themeButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.body.className = btn.dataset.theme;
    });
  });

  const settingSpeedLimit = document.getElementById('settingSpeedLimit');
  const lblSettingSpeedLimit = document.getElementById('lblSettingSpeedLimit');
  settingSpeedLimit.addEventListener('input', (e) => {
    lblSettingSpeedLimit.textContent = `Batas Peringatan Kecepatan (Tol): ${e.target.value} KM/H`;
  });

  const cockpitBrandBadge = document.getElementById('cockpitBrandBadge');
  const settingBrandInput = document.getElementById('settingBrandInput');

  document.getElementById('btnSaveSettings').addEventListener('click', () => {
    // Save Custom Brand Name
    const customBrand = settingBrandInput.value.trim() || 'SANEPO';
    if (cockpitBrandBadge) {
      cockpitBrandBadge.textContent = `✦ ${customBrand.toUpperCase()} ✦`;
    }

    speedLimit = parseInt(settingSpeedLimit.value);
    speedLimitText.textContent = `BATAS: ${speedLimit} KM/H`;
    updateSpeed(speed);

    const newOdo = parseInt(document.getElementById('settingOdoInput').value);
    if (!isNaN(newOdo) && newOdo > 0) {
      odoTotal = newOdo;
      document.getElementById('odoText').textContent = `ODO: ${odoTotal.toLocaleString('id-ID')} KM`;
    }
    closeModal(modalSettings);
    alert(`Pengaturan disimpan! Branding kokpit: ${customBrand}`);
  });

  const btnSimCheckUpdate = document.getElementById('btnSimCheckUpdate');
  const simUpdateFeedback = document.getElementById('simUpdateFeedback');
  if (btnSimCheckUpdate) {
    btnSimCheckUpdate.addEventListener('click', () => {
      simUpdateFeedback.textContent = 'Memeriksa GitHub krisdwiantara12/innova2kd...';
      setTimeout(() => {
        simUpdateFeedback.textContent = 'Aplikasi sudah versi terbaru (v1.1.0-OEM)!';
      }, 800);
    });
  }

  // Volume Slider
  const volSlider = document.getElementById('volSlider');
  const btnMute = document.getElementById('btnMute');
  let isMuted = false;
  let prevVol = 9;

  btnMute.addEventListener('click', () => {
    isMuted = !isMuted;
    if (isMuted) {
      prevVol = volSlider.value;
      volSlider.value = 0;
      btnMute.textContent = 'UNMUTE';
    } else {
      volSlider.value = prevVol;
  // OBD2 Simulation Toggle
  const btnToggleSimObd = document.getElementById('btnToggleSimObd');
  const simObdTelemetry = document.getElementById('simObdTelemetry');
  let isObdConnected = false;

  if (btnToggleSimObd && simObdTelemetry) {
    btnToggleSimObd.addEventListener('click', () => {
      isObdConnected = !isObdConnected;
      if (isObdConnected) {
        simObdTelemetry.style.display = 'flex';
        simObdTelemetry.style.opacity = '0';
        setTimeout(() => { simObdTelemetry.style.opacity = '1'; }, 50);
        btnToggleSimObd.textContent = '🔌 CABUT ELM327 OBD2 (ONLINE)';
        btnToggleSimObd.style.borderColor = '#10b981';
        btnToggleSimObd.style.color = '#10b981';
      } else {
        simObdTelemetry.style.opacity = '0';
        setTimeout(() => { simObdTelemetry.style.display = 'none'; }, 300);
        btnToggleSimObd.textContent = '⚡ TEST SAMBUNG ELM327 OBD2';
        btnToggleSimObd.style.borderColor = 'rgba(56,189,248,0.3)';
        btnToggleSimObd.style.color = '#38bdf8';
      }
    });
  }

});
