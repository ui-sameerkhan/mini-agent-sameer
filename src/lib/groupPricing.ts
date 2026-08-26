/**
 * Group-deal pricing: every service+date forms a group. Each time someone new
 * books into that group, everyone who joined earlier gets a further discount.
 * Whoever joins last always pays closest to full price at that moment — but
 * their price will drop too if more people join after them later.
 */
export const GROUP_STEP_PERCENT = 0.05; // price drop per later joiner, as a fraction of base price
export const GROUP_MAX_DISCOUNT_PERCENT = 0.5; // never discount more than this fraction of base price

export function groupMemberPrice(
  basePrice: number,
  groupSize: number,
  joinOrder: number,
): number {
  const laterJoiners = Math.max(0, groupSize - joinOrder);
  const rawPrice = basePrice - basePrice * GROUP_STEP_PERCENT * laterJoiners;
  const floor = basePrice * (1 - GROUP_MAX_DISCOUNT_PERCENT);
  return Math.round(Math.max(rawPrice, floor) * 100) / 100;
}
