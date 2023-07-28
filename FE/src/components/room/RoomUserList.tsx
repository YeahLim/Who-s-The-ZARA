import yellowBtnImg from "../../assets/img/yellowBtnImg.png";
import { Link } from "react-router-dom";
import { RoomUserListItem } from "./RoomUserListItem";

export const RoomUserList = () => {
  const num = [...Array(8).keys()];
  return (
    <>
      <div className="relative 3xl:w-[1140px] w-[912px] 3xl:h-[700px] h-[560px] border-solid border-white 3xl:border-[20px] border-[16px] 3xl:p-[24px] p-[19.2px] 3xl:text-[56px] text-[44.8px] font-bold bg-black">
        <div className="flex flex-wrap">
          {num.map((item, index) => (
            <RoomUserListItem item={item} key={index} />
          ))}
        </div>
        <div className="absolute 3xl:w-[360px] w-[288px] 3xl:h-[120px] h-[96px] flex justify-center items-center bottom-[-50px] right-[-60px]">
          <img src={yellowBtnImg} className="absolute" />
          {/* TODO: ingame으로 진입 및 ingame에서 방 세팅 보내면서 openvidu 시작*/}
          <Link to="/game" className="absolute w-full text-center py-[20px]">
            게임시작
          </Link>
        </div>
      </div>
    </>
  );
};