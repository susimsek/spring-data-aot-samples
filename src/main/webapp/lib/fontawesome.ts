import { config } from '@fortawesome/fontawesome-svg-core';
import '@fortawesome/fontawesome-svg-core/styles.css';
import { loadIcons } from './iconLoader';

config.autoAddCss = false;
loadIcons();
