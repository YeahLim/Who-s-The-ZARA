import StoryImg from "../../assets/img/home/StoryImg.png";

const StoryContent = () => {
  return (
    <div className="text-center text-[28px] leading-[60px]">
      <p>아픈 용왕을 위해 토끼의 간을 찾으러 나선 자라...</p>
      <p>자라는 토끼로 변장했다!</p>
      <p>하지만 토끼는 자라들이 숨어들어 왔다는 것을 알게 되었고...</p>
      <p>그렇게 토끼와 자라의 숨막히는 혈전이 시작된다!</p>
      <img className="m-auto h-[220px]" src={StoryImg} />
    </div>
  );
};
export default StoryContent;
