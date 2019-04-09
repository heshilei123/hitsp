package com.hisense.hitsp.third;

import com.hisense.hitsp.third.eb.EbImpl;
import com.hisense.hitsp.third.eleme.ElemeImpl;
import com.hisense.hitsp.third.jingdong.JingDongImpl;
import com.hisense.hitsp.third.meituan.MeituanImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * 统一处理外卖平台逻辑
 *
 * @author codepsi
 */
@Component
public class PlatFormFactory {

    private static ApplicationContext context;

    public PlatFormFactory(ApplicationContext context) {
        PlatFormFactory.context = context;
    }

    /**
     * 判断平台来源，返回IPlatform实例
     *
     * @param src
     * @return
     */
    public static IPlatform getPlatform(String src) {
        IPlatform iPlatform = null;
        switch (src) {
            case "11":
                //TODO 返回BaiduImpl
                break;
            case "12":
                iPlatform = context.getBean(MeituanImpl.class);//美团
                break;
            case "15":
                iPlatform = context.getBean(ElemeImpl.class);//饿了么
                break;
            case "16":
                iPlatform = context.getBean(JingDongImpl.class);//京东到家
                break;
            case "17":
                iPlatform = context.getBean(EbImpl.class);//饿百
                break;
            default:
                iPlatform = context.getBean(MeituanImpl.class);
                break;
        }

        return iPlatform;
    }

    /**
     * 获取所有平台实例
     *
     * @return
     */
    public static List<IPlatform> getAllPlatform() {
        List<IPlatform> platformList = new LinkedList<>();
        platformList.add(context.getBean(MeituanImpl.class));
        platformList.add(context.getBean(ElemeImpl.class));
        platformList.add(context.getBean(EbImpl.class));
        platformList.add(context.getBean(JingDongImpl.class));
        return platformList;
    }

    /**
     * 获取类京东的强制公共商品管理的平台
     * @return
     */
    public static List<IPlatform> getCompelPublicPlatform() {
        List<IPlatform> platformList = new LinkedList<>();
        platformList.add(context.getBean(JingDongImpl.class));
        return platformList;
    }

}
