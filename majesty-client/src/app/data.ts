export interface Game {
  players: Player[];
  bSide: boolean;
  cardsQueue: CardInQueue[];
}

export interface CardInQueue {
  card: Card;
  meeples: number;
}

export type Place = "MILL" | "BREWERY" | "COTTAGE" | "GUARDHOUSE" | "BARRACKS" | "INN" | "CASTLE";

export interface Player {
  name: string;
  score: number;
  meeples: number;
  cards: {
    MILL: Card[],
    BREWERY: Card[],
    COTTAGE: Card[],
    GUARDHOUSE: Card[],
    BARRACKS: Card[],
    INN: Card[],
    CASTLE: Card[],
  };
  infirmary: Card[];
}

export interface Card {
  places: Place[];
}

export interface Notification {
  type: string;
  playerName: string;
  notification: any;
}

export function messageNotification(notification: Notification) {
  return notification.notification as {message: string};
}
export function cardTakenNotification(notification: Notification) {
  return notification.notification as {card: Card, meeplesPlaced: number, meeplesGained: number};
}
export function millPlayedNotification(notification: Notification) {
  return notification.notification as {score: number};
}
export function breweryPlayedNotification(notification: Notification) {
  return notification.notification as {score: number, meeples: number, benefitedPlayerNames: string[]};
}
export function cottagePlayedNotification(notification: Notification) {
  return notification.notification as {healed: Place | null, score: number};
}
export function guardhousePlayedNotification(notification: Notification) {
  return notification.notification as {guards: number, score: number};
}
export function barracksPlayedNotification(notification: Notification) {
  return notification.notification as {attackStrength: number, attackedPlayers: {name: string, place: Place}[], score: number};
}
export function innPlayedNotification(notification: Notification) {
  return notification.notification as {score: number, benefitedPlayerNames: string[]};
}
export function castlePlayedNotification(notification: Notification) {
  return notification.notification as {score: number, meeples: number};
}
export function meeplesSoldNotification(notification: Notification) {
  return notification.notification as {amount: number};
}
export function finalScoringNotification(notification: Notification) {
  return notification.notification as {varietyScore: number, maxCards: {place: Place, score: number}[], infirmaryScore: number};
}
