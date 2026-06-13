package com.example.springaichat.tool;

import com.example.springaichat.model.ClassInfo;
import com.example.springaichat.repository.ClassInfoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级信息查询工具
 * 用于查询班级班主任信息和学生人数
 */
@Component
public class ClassInfoTool {

    private final ClassInfoRepository classInfoRepository;

    public ClassInfoTool(ClassInfoRepository classInfoRepository) {
        this.classInfoRepository = classInfoRepository;
    }

    /**
     * 查询所有班级信息
     * @return 所有班级的班主任和人数信息
     */
    public String getAllClassInfo() {
        List<ClassInfo> classInfos = classInfoRepository.findAll();
        if (classInfos.isEmpty()) {
            return "暂无班级信息";
        }

        StringBuilder result = new StringBuilder("班级信息如下：\n");
        for (ClassInfo info : classInfos) {
            result.append(String.format("• %s：班主任是 %s，人数 %d 人\n",
                    info.className(),
                    info.homeroomTeacher(),
                    info.studentCount()));
        }
        return result.toString().trim();
    }

    /**
     * 根据班级名称查询班级信息
     * @param className 班级名称，如"一班"、"二班"
     * @return 该班级的班主任和人数信息
     */
    public String getClassInfoByName(String className) {
        ClassInfo info = classInfoRepository.findByClassName(className);
        if (info == null) {
            // 尝试模糊查询
            List<ClassInfo> matched = classInfoRepository.searchByClassName(className);
            if (matched.isEmpty()) {
                return "未找到班级「" + className + "」的信息";
            }
            // 如果模糊查询只有一个结果，直接返回
            if (matched.size() == 1) {
                info = matched.get(0);
            } else {
                // 返回所有匹配的班级
                return matched.stream()
                        .map(c -> String.format("• %s：班主任是 %s，人数 %d 人",
                                c.className(), c.homeroomTeacher(), c.studentCount()))
                        .collect(Collectors.joining("\n"));
            }
        }

        return String.format("班级「%s」的信息：\n• 班主任：%s\n• 人数：%d 人",
                info.className(),
                info.homeroomTeacher(),
                info.studentCount());
    }

    /**
     * 查询班主任信息
     * @param teacherName 班主任姓名
     * @return 该班主任对应的班级信息
     */
    public String getClassByTeacher(String teacherName) {
        List<ClassInfo> allClasses = classInfoRepository.findAll();
        List<ClassInfo> matched = allClasses.stream()
                .filter(c -> c.homeroomTeacher().toLowerCase().contains(teacherName.toLowerCase()))
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            return "未找到班主任「" + teacherName + "」的班级信息";
        }

        return matched.stream()
                .map(c -> String.format("• %s：班主任是 %s，人数 %d 人",
                        c.className(), c.homeroomTeacher(), c.studentCount()))
                .collect(Collectors.joining("\n"));
    }
}
