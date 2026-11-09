// WMO šifre vremena s Open-Meteo u ikonu i kratki opis
export function opisVremena(kod: number): { ikona: string; opis: string } {
  if (kod === 0) return { ikona: '☀️', opis: 'vedro' };
  if (kod === 1) return { ikona: '🌤️', opis: 'pretežno vedro' };
  if (kod === 2) return { ikona: '⛅', opis: 'djelomično oblačno' };
  if (kod === 3) return { ikona: '☁️', opis: 'oblačno' };
  if (kod === 45 || kod === 48) return { ikona: '🌫️', opis: 'magla' };
  if (kod >= 51 && kod <= 57) return { ikona: '🌦️', opis: 'rosulja' };
  if (kod >= 61 && kod <= 67) return { ikona: '🌧️', opis: 'kiša' };
  if (kod >= 71 && kod <= 77) return { ikona: '🌨️', opis: 'snijeg' };
  if (kod >= 80 && kod <= 82) return { ikona: '🌦️', opis: 'pljuskovi' };
  if (kod === 85 || kod === 86) return { ikona: '🌨️', opis: 'snježni pljuskovi' };
  if (kod >= 95) return { ikona: '⛈️', opis: 'grmljavina' };
  return { ikona: '🌡️', opis: '' };
}
