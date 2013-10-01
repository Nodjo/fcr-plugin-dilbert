package nodjo.fcr.comics;

interface IDailyComic {
	
    long getFirstDate();    
    String getStripUrl(long date);
    
    // The online store to allow the user to buy comic-related products
    // If no online store is available, return the website url
    String getWebsiteUrl();
}