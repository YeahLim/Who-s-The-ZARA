import rabbitImg from "../../assets/img/rabbitImg.png";
import btnImg from "../../assets/img/blackBtnImg.png";
import { useState } from "react";

interface lobbySideMenuProps {
  onSetViewMain: (num: number) => void;
  viewMain: number;
}

const LobbySideMenu = ({ viewMain, onSetViewMain }: lobbySideMenuProps) => {
  const [roomNum, setRoomNum] = useState("");

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRoomNum(e.target.value);
  };

  return (
    <aside className="ml-[40px] flex flex-col leading-[140px] text-center">
      <div
        className={`3xl:w-[400px] w-[300px] 3xl:h-[200px] h-[150px] bg-contain bg-no-repeat bg-center relative flex items-center justify-center cursor-pointer`}
        style={{ backgroundImage: `url(${btnImg})` }}
        onClick={() => onSetViewMain(1)}
      >
        <p className={`text-white 3xl:text-[48px] text-[36px] w-full ${viewMain === 1 ? "text-yellow-200" : ""}`}>
          방 만들기
        </p>
      </div>
      <div
        className={`3xl:w-[400px] w-[300px] 3xl:h-[200px] h-[150px] bg-contain bg-no-repeat bg-center relative flex items-center justify-center cursor-pointer`}
        style={{ backgroundImage: `url(${btnImg})` }}
        onClick={() => onSetViewMain(2)}
      >
        <p className={`text-white 3xl:text-[48px] text-[36px] w-full ${viewMain === 2 ? "text-yellow-200" : ""}`}>
          방 찾기
        </p>
      </div>
      <div
        className={`3xl:w-[400px] w-[300px] 3xl:h-[200px] h-[150px] bg-contain bg-no-repeat bg-center relative flex items-center justify-center`}
        style={{ backgroundImage: `url(${btnImg})` }}
      >
        <input
          type="text"
          className="3xl:w-[340px] w-[250px] 3xl:h-[90px] h-[60px] 3xl:text-[48px] text-[36px] text-center bg-black text-white underline"
          placeholder="방 번호 입력"
          maxLength={6}
          onChange={onChange}
          value={roomNum}
        ></input>
      </div>
      <img
        src={rabbitImg}
        className="absolute z-index-5 3xl:left-[120px] left-[100px] 3xl:top-[-130px] top-[-84px] 3xl:w-[260px] w-[200px]"
      />
    </aside>
  );
};

export default LobbySideMenu;
