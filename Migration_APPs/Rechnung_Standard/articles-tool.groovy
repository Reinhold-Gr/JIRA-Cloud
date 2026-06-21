// =====================================================================
// articles-tool.groovy — CLOUD VERSION
// =====================================================================
// MIGRATIONSHINWEIS:
// Dieses Script ist UNVERÄNDERT gegenüber der DC-Version.
// Es enthält ausschließlich reine Java/Groovy-Logik (DecimalFormat,
// String-Parsing, Preis-/Steuerberechnung) und KEINE Jira- oder
// ComponentAccessor-Aufrufe. Daher 1:1 in die Cloud übernehmbar.
// =====================================================================

import java.text.DecimalFormat;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

articlesTool = new ArticlesTool()

class ArticlesTool{

  	def totalPrice = 0
  	def tax = 0


	final DecimalFormat df = setDecimalFormat();

  	def setDecimalFormat(){
    	def Locale currentLocale = Locale.getDefault();

  		def DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(currentLocale);
      	def char comma = ','
      	def char dot = '.'
		otherSymbols.setDecimalSeparator(comma);
  		otherSymbols.setGroupingSeparator(dot);
		DecimalFormat decfor = new DecimalFormat("#0.00", otherSymbols);
      	return decfor;
  	}

  	def formatNumber(number){
    	return df.format(number)
  	}


  	//We receive a string representing the table of articles with the next format: ||art||koste|| | first|100| |second|200|
	def parse(stringArticles) {
      String subStringName = new String();
      def subStringPrice = ""
      def artikelPrice = 0;
      def index1 = stringArticles.indexOf("|", stringArticles.lastIndexOf("||")+2)
      def index2 = stringArticles.indexOf("|", index1+1)
      def articles = []

      while(index1 > -1){
          subStringName = stringArticles.substring(index1+1, index2)
          subStringName = removeSpacesAscii(subStringName.trim())

          index1 = index2
          index2 = stringArticles.indexOf("|", index1+1)

          subStringPrice = stringArticles.substring(index1+1, index2)
          subStringPrice = removeSpacesAscii(subStringPrice.trim())

          if(!isNumeric(subStringPrice)){
            subStringPrice = 0;
          }

          artikelPrice = df.format(Double.valueOf(subStringPrice));
          subStringPrice = artikelPrice.toString();

          articles.add([ name: subStringName, price: subStringPrice ])
          totalPrice += Double.valueOf(subStringPrice)

          index1 = stringArticles.indexOf("|", index2+1)
      	  index2 = stringArticles.indexOf("|", index1+1)
        }

      return articles
	}

  def getTotalPrice(){
   	return df.format(totalPrice)
  }

  def getTaxes(steuersatz){
    tax = Integer.parseInt(steuersatz) * totalPrice / 100
    return df.format(tax)
  }

  def getFinalPrice(){
   return df.format(totalPrice + tax)
  }

  def removeSpacesAscii(str){
    if(str.isEmpty() || str == null || str.length() <= 1){
     return "-";
    }
    while(((int)str.charAt(0)) == 160){
     str = str.substring(1);
    }
    while(((int)str.charAt(str.length()-1)) == 160 || ((int)str.charAt(str.length()-1)) == 8364){
     str = str.substring(0, str.length()-1);
    }
    return str
  }

  public static boolean isNumeric(String strNum) {
    try {
        double d = Double.parseDouble(strNum);
    } catch (Exception e) {
        return false;
    }
    return true;
  }

  //CALCULATIONS IN AUFTRAG
  def totalPriceAuftrag = 0

  def getTaxesAuftrag(price, steuersatz){
    tax = Integer.parseInt(steuersatz) * price / 100
    totalPriceAuftrag = price + tax
    return df.format(tax)
  }

  def getTotalPriceAuftrag(){
   	return df.format(totalPriceAuftrag)
  }


  //CALCULATIONS IN RECHNUNG
  def totalPriceRechnung = 0

  def calculateTotalPriceRechnung(price1, price2, price3, price4, price5, price6){
      if(price1 != null){
          totalPriceRechnung =  totalPriceRechnung + price1;
      }
      if(price2 != null){
          totalPriceRechnung =  totalPriceRechnung + price2;
      }
      if(price3 != null){
          totalPriceRechnung =  totalPriceRechnung + price3;
        }
      if(price4 != null){
          totalPriceRechnung =  totalPriceRechnung + price4;
      }
      if(price5 != null){
          totalPriceRechnung =  totalPriceRechnung + price5;
      }
      if(price6 != null){
          totalPriceRechnung =  totalPriceRechnung + price6;
      }

      return df.format(totalPriceRechnung)
  }

  def getTotalPriceRechnung(){
    	return df.format(totalPriceRechnung)
  }

  def getTaxesRechnung(steuersatz){
      tax = Integer.parseInt(steuersatz) * totalPriceRechnung / 100
      return df.format(tax)
  }

  def getFinalPriceRechnung(){
   		return df.format(totalPriceRechnung + tax)
  }
}
