import roomChat from "../../assets/img/room/roomChat.png";
import { useState } from "react";
import { useWebSocket } from "../../context/socketContext";
import { useAccessTokenState } from "../../context/accessTokenContext";
import { useParams } from "react-router-dom";
import { PubChat } from "../../types/StompRoomPubType";
import { SFX, playSFX } from "../../utils/audioManager";
import { ChatInfo } from "../../types/StompRoomSubType";
import { TEXT_COLOR_MAP } from "../../constants/common/TextColorMap";

interface RoomChatProps {
  chatList: ChatInfo[];
}

export const RoomChat = ({ chatList }: RoomChatProps) => {
  const [inputChat, setInputChat] = useState("");
  const { roomCode } = useParams<{ roomCode: string }>();
  const { client } = useWebSocket();
  const { userSeq } = useAccessTokenState();

  const sendChat = () => {
    if (!roomCode) return;
    playSFX(SFX.CLICK);
    const body: PubChat = {
      senderSeq: userSeq,
      message: inputChat,
    };
    client?.publish({
      destination: `/pub/room/${roomCode}/chat`,
      body: JSON.stringify(body),
    });
    setInputChat("");
  };

  return (
    <aside className="relative 3xl:mb-[30px] mb-[24px] 3xl:w-[550px] w-[440px] 3xl:h-[720px] h-[576px] text-white 3xl:ml-[25px] ml-[20px]">
      <img src={roomChat} className="absolute left-[0px] top-[0px] w-[full]" />
      <div className="absolute 3xl:top-[60px] top-[48px] 3xl:left-[40px] left-[36px] 3xl:text-[28px] text-[20.4px] 3xl:pr-[10px] pr-[8px] overflow-y-scroll 3xl:h-[540px] h-[432px] 3xl:w-[490px] w-[392px]">
        {chatList.map((chatInfo, index) => (
          <div>
            <span key={index} className={`TEXT_COLOR_MAP[chatInfo.order+1]`}>
              {chatInfo.nickname}
            </span>

            <span key={index}>{chatInfo.message}</span>
          </div>
        ))}
      </div>
      <input
        className="absolute 3xl:w-[510px] w-[408px] 3xl:h-[60px] h-[48px] 3xl:left-[20px] left-[16px] 3xl:bottom-[20px] bottom-[16px] text-black 3xl:px-[20px] px-[16px] 3xl:text-[28px] text-[22.4px]"
        value={inputChat}
        onChange={(e) => setInputChat(e.target.value)}
        onKeyUp={(e) => {
          if (e.key === "Enter") {
            sendChat();
          }
        }}
      />
    </aside>
  );
};
