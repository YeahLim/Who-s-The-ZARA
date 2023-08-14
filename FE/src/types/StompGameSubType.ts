type userSeq = number;
type jobSeq = number;
type nickname = string;

export interface SubStart {
  type: "START";
  data: {
    userSeq: userSeq;
    jobSeq: jobSeq;
    nickname: nickname;
  }[];
}
export interface SubChat {
  type: "CHAT";
  gameCode: number;
  data: {
    sender: userSeq;
    nickname: string;
    message: string;
  };
}

export interface SubStartTimer {
  type: "TIMER";
  data: {
    type: string;
    time: number;
  };
}

export interface SubCharLoc {
  type: "GAME_CHAR_LOC";
  data: {
    orderNumber: number;
    xaxis1: number;
    yaxis1: number;
    xaxis2: number;
    yaxis2: number;
  };
}

export interface SubVote {
  type: "VOTE";
  data: {
    userSeq: number;
    cnt: number;
  }[];
}

export interface SubVoteResult {
  type: "VOTE_RESULT";
  data: userSeq;
}

export interface SubNightResult {
  type: "NIGHT_RESULT";
  data: {
    deadUserSeq: userSeq | null;
    threatUserSeq: userSeq | null;
    healUserSeq: userSeq | null;
    ability: { userSeq: number; targetUserSeq: number | null; result: boolean }[];
  };
}

export interface SubGameResult {
  type: "GAME_OVER";
  data: {
    rabbitWin: boolean;
    userInfo: {
      userSeq: userSeq;
      jobSeq: jobSeq;
      win: boolean;
      nickname: string;
    }[];
  };
}

export interface SubZaraChat {
  type: "ZARA_CHAT";
  gameCode: number;
  data: {
    sender: userSeq;
    nickname: string;
    message: string;
  };
}

export interface SubGhostChat {
  type: "GHOST_CHAT";
  gameCode: number;
  data: {
    sender: userSeq;
    nickname: string;
    message: string;
  };
}

export interface SubZaraTarget {
  type: "ABILITY";
  gameCode: number;
  data: number;
}
