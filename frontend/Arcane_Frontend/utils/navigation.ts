export const getSummonerUrl = (summonerName: string) => {
  const urlName = encodeURIComponent(summonerName.replace("#", "-"));
  return `/summoner/${urlName}`;
};
