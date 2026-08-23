import type { LevelCrossing } from '../types'

/**
 * SAMPLE DATA. Illustrative level crossings placed along the modelled
 * section. Gate numbers, road names and coordinates are fictionalised for
 * this demo and do not correspond to real Indian Railways LC gate records.
 */
export const crossings: LevelCrossing[] = [
  {
    id: 'lc-mathura-road',
    gateNumber: 'LC-05',
    name: 'Mathura Road Crossing',
    road: 'NH19 Service Road',
    fromStation: 'NDLS',
    toStation: 'FDB',
    km: 12,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 28.4595,
    lng: 77.2926,
  },
  {
    id: 'lc-ballabhgarh',
    gateNumber: 'LC-12',
    name: 'Ballabhgarh Link Road Crossing',
    road: 'Ballabhgarh–Sohna Road',
    fromStation: 'FDB',
    toStation: 'PWL',
    km: 15,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 28.3389,
    lng: 77.3212,
  },
  {
    id: 'lc-hodal-road',
    gateNumber: 'LC-34',
    name: 'Hodal Road Crossing',
    road: 'Hodal–Palwal Road',
    fromStation: 'PWL',
    toStation: 'MTJ',
    km: 10,
    closeBeforeMin: 6,
    clearanceBufferSec: 60,
    lat: 28.0967,
    lng: 77.3639,
  },
  {
    id: 'lc-chhata-road',
    gateNumber: 'LC-41',
    name: 'Chhata Road Crossing',
    road: 'Chhata–Kosi Kalan Road',
    fromStation: 'PWL',
    toStation: 'MTJ',
    km: 60,
    closeBeforeMin: 6,
    clearanceBufferSec: 60,
    lat: 27.7167,
    lng: 77.4667,
  },
  {
    id: 'lc-vrindavan-road',
    gateNumber: 'LC-58',
    name: 'Vrindavan Road Crossing',
    road: 'Mathura–Vrindavan Road',
    fromStation: 'MTJ',
    toStation: 'AGC',
    km: 20,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 27.5200,
    lng: 77.6800,
  },
]
