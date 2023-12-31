import { Link } from "react-router-dom";
import { useWebSocket } from "../../context/socketContext";
import { useParams } from "react-router-dom";
import { CurSeats } from "../../types/RoomSettingType";
import { toast } from "react-toastify";
import { SFX, playSFX } from "../../utils/audioManager";
import { useAccessTokenState } from "../../context/accessTokenContext";

interface RoomHeaderBtnProps {
  amIOwner: boolean;
  curSeats: CurSeats;
}

export const RoomHeaderBtn = ({ amIOwner, curSeats }: RoomHeaderBtnProps) => {
  const { client } = useWebSocket();
  const { roomCode } = useParams();
  const { accessToken } = useAccessTokenState();

  const onClickStart = () => {
    const occupiedSeatsCnt = curSeats.filter((seat) => seat.state === 1).length;
    if (occupiedSeatsCnt < 5) {
      toast.error("5명 이상의 플레이어가 필요합니다.");
      playSFX(SFX.ERROR);
      return;
    }
    playSFX(SFX.CLICK);
    console.log(curSeats);

    client?.publish({
      destination: `/pub/room/${roomCode}/start`,
    });
  };

  const pubExitRoom = (roomCode: string) => {
    client?.publish({
      destination: `/pub/room/${roomCode}/exit`,
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
  };

  const onClickExit = (roomCode: string | undefined) => {
    if (!roomCode) return;
    pubExitRoom(roomCode);
    playSFX(SFX.CLICK);
  };

  return (
    <div className={`3xl:w-[360px] w-[288px] 3xl:h-[100px] h-[80px] bg-cover flex items-center font-bold`}>
      {amIOwner ? (
        <div
          onClick={onClickStart}
          className="hover:border-amber-200  3xl:text-[30px] text-[24px] 3xl:w-[150px] w-[120px] 3xl:py-[20px] py-[16px] text-center border-white 3xl:border-[10px] border-[8px] bg-black 3xl:ml-[20px] ml-[16px] text-yellow-400 duration-500 transition-colors"
        >
          Start
        </div>
      ) : (
        <div className="3xl:text-[30px] text-[24px] 3xl:w-[150px] w-[120px] 3xl:py-[20px] py-[16px] text-center border-white 3xl:border-[10px] border-[8px] bg-black 3xl:ml-[20px] ml-[16px] text-gray-500">
          Start
        </div>
      )}
      <Link
        to="/lobby"
        className="  hover:border-amber-200 3xl:text-[30px] text-[24px] 3xl:w-[150px] w-[120px] 3xl:py-[20px] py-[16px] text-center border-white 3xl:border-[10px] border-[8px] bg-black 3xl:ml-[20px] ml-[16px] text-red-400 duration-500 transition-colors"
        onClick={() => onClickExit(roomCode)}
      >
        Exit
      </Link>
    </div>
  );
};
