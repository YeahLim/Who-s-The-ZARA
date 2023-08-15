import { useState } from "react";
import gameChatImg from "../../assets/img/game/gameChatImg.png";
import { GameChatContent } from "./GameChatContent";
import { GameChatInput } from "./GameChatInput";
import { GameChatTab } from "./GameChatTab";
import { SFX, playSFX } from "../../utils/audioManager";

interface Chat {
  userOrder: number;
  nickname: string;
  message: string;
}
interface GameChatProps {
  allChatList: Chat[];
  zaraChatList: Chat[];
  ghostChatList: Chat[];
  myJobSeq: number;
  amIDead: boolean;
  amIZara: boolean;
  nowTime: string;
}

export const GameChat = ({ nowTime, allChatList, zaraChatList, ghostChatList, amIDead, amIZara }: GameChatProps) => {
  const [selectTab, setSelectTab] = useState(0);
  const onSetSelectTab = (index: number) => {
    playSFX(SFX.TAB);
    setSelectTab(index);
  };

  return (
    <div
      className="absolute 3xl:top-[312px] top-[250px] left-0 3xl:w-[350px] w-[280px] 3xl:h-[350px] h-[280px] bg-cover opacity-80"
      style={{ backgroundImage: `url(${gameChatImg})` }}
    >
      <GameChatTab selectTab={selectTab} onSetSelectTab={onSetSelectTab} amIDead={amIDead} amIZara={amIZara} />

      {selectTab === 0 && (
        <div>
          <GameChatContent chatList={allChatList} />
          <GameChatInput chatTabCategory={0} amIDead={amIDead} nowTime={nowTime} />
        </div>
      )}
      {selectTab === 1 && (
        <div>
          <GameChatContent chatList={zaraChatList} />
          <GameChatInput chatTabCategory={1} amIDead={amIDead} nowTime={nowTime} />
        </div>
      )}
      {selectTab === 2 && (
        <div>
          <GameChatContent chatList={ghostChatList} />
          <GameChatInput chatTabCategory={2} amIDead={amIDead} nowTime={nowTime} />
        </div>
      )}
    </div>
  );
};
