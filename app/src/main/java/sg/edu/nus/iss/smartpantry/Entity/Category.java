package sg.edu.nus.iss.smartpantry.Entity;

/**
 * Created by CHARAN on 5/8/2015.
 */
public class Category {
    private String categoryName;

    private int categoryId;
    public Category(int categoryId){
        this.categoryId=categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    public int getCategoryId() {
        return categoryId;
    }
}
