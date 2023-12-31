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
  cards: Map<Place, Card[]>;
  infirmary: Card[];
}

export interface Card {
  places: Place[];
}
