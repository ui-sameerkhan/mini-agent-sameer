import type { LevelCrossing } from '../types'

/**
 * SAMPLE DATA. Illustrative level crossings inside Barabanki city, on the
 * three lines radiating from Barabanki Junction. Gate numbers, road names
 * and coordinates are fictionalised for this demo and do not correspond to
 * real Indian Railways LC gate records.
 */
export const crossings: LevelCrossing[] = [
  {
    id: 'lc-lucknow-road',
    gateNumber: 'LC-07',
    name: 'Lucknow Road Crossing',
    road: 'Lucknow Road (NH27)',
    fromStation: 'LKO',
    toStation: 'BBK',
    km: 24,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 26.943,
    lng: 81.178,
  },
  {
    id: 'lc-ayodhya-road',
    gateNumber: 'LC-14',
    name: 'Ayodhya Road Crossing',
    road: 'Ayodhya Road',
    fromStation: 'BBK',
    toStation: 'RDL',
    km: 4,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 26.931,
    lng: 81.205,
  },
  {
    id: 'lc-fatehpur-road',
    gateNumber: 'LC-21',
    name: 'Fatehpur Road Crossing',
    road: 'Fatehpur Road',
    fromStation: 'BBK',
    toStation: 'SFG',
    km: 3,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 26.925,
    lng: 81.185,
  },
  {
    id: 'lc-ram-sanehi-ghat-road',
    gateNumber: 'LC-26',
    name: 'Ram Sanehi Ghat Road Crossing',
    road: 'Ram Sanehi Ghat Road',
    fromStation: 'BBK',
    toStation: 'SFG',
    km: 9,
    closeBeforeMin: 6,
    clearanceBufferSec: 60,
    lat: 26.915,
    lng: 81.195,
  },
  {
    // Coordinates from a location shared by the user (a Google Maps pin,
    // no place name attached). Placed on the LKO-BBK line since that's the
    // section it geographically sits nearest to; road name is a
    // placeholder — rename it once you know the actual crossing/road name.
    id: 'lc-user-pinned',
    gateNumber: 'LC-33',
    name: 'Pinned Crossing (near Barabanki town)',
    road: 'Unnamed local road — rename once confirmed',
    fromStation: 'LKO',
    toStation: 'BBK',
    km: 26,
    closeBeforeMin: 5,
    clearanceBufferSec: 45,
    lat: 26.9323419,
    lng: 81.175884,
  },
]

/** Barabanki Junction itself — shown on the map for orientation. */
export const barabankiJunction = { name: 'Barabanki Junction', lat: 26.9385, lng: 81.192 }

/** Map center/zoom for the city view. */
export const cityView = { center: [26.9339, 81.1836] as [number, number], zoom: 13 }
