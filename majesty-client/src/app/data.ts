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
