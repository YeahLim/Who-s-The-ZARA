import { RoomHeader } from "../components/room/RoomHeader";
import { RoomExitBtn } from "../components/room/RoomExitBtn";
import { RoomChat } from "../components/room/RoomChat";
import { RoomUserList } from "../components/room/RoomUserList";
import { RoomLayout } from "../layouts/RoomLayout";

export const Room = () => {
  return (
    <RoomLayout>
      <div className="flex flex-wrap w-full justify-center items-center 3xl:px-[40px] px-[36px]">
        <div className="flex justify-around items-center w-full">
          <RoomHeader />
          <RoomExitBtn />
        </div>
        <div className="flex justify-around w-full items-center">
          <RoomChat />
          <RoomUserList />
        </div>
      </div>
    </RoomLayout>
  );
};