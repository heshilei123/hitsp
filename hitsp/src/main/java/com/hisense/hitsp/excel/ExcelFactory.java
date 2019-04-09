package com.hisense.hitsp.excel;

import com.hisense.hitsp.third.IPlatform;
import com.hisense.hitsp.third.PlatFormFactory;
import com.hisense.hitsp.third.eleme.ElemeImpl;
import com.hisense.hitsp.third.meituan.MeituanImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by user on 2017/10/24.
 */
@Component
public class ExcelFactory {
    private static ApplicationContext context;

    public ExcelFactory(ApplicationContext context) {
        ExcelFactory.context = context;
    }

    /**
     * 判断excel来源，返回excel实例
     * @param type
     * @return
     */
    public static Excel getPlatform(String type) {
        Excel excel = null;
        switch (type) {
            case "shop":
                excel = context.getBean(ShopExcelImpl.class);
                break;
            case "category":
                excel = context.getBean(CategoryExcelImpl.class);
                break;
            case "public_category":
                excel = context.getBean(PublicCategoryExcelImpl.class);
                break;
            case "product":
                excel = context.getBean(ProductExcelImpl.class);
                break;
            case "public_product":
                excel = context.getBean(PublicProductExcelImpl.class);
            default:
                break;
        }

        return excel;
    }
}
