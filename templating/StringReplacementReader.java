package de.danielhons.lib.templating;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static de.danielhons.lib.templating.Replaces.Type;

@Slf4j
public class StringReplacementReader implements ReplacementReader<String> {

    @Override
    public Map<String, String> readReplacements(Object o) {
        Map<String, String> replacements = new HashMap<>();
        addToReplacements(o, replacements);
        return replacements;
    }


    private void addToReplacements(Object obj, Map<String, String> replacements) {
        if (obj == null) return;
        processObject(obj, replacements);

    }

    private void processObject(Object obj, Map<String, String> replacements) {
        Class cls = obj.getClass();
        processClass(cls, obj, replacements);
        while ((cls = cls.getSuperclass()) != null) {
            processClass(cls, obj, replacements);
        }
    }

    private void processClass(Class cls, Object obj, Map<String, String> replacements) {
        processFields(cls,obj, replacements);
        processMethods(cls,obj, replacements);
    }


    private void processFields(Class cls,Object obj, Map<String, String> replacements) {
        Field[] fields = cls.getDeclaredFields();
        processFields(fields, obj, replacements);
    }

    private void processFields(Field[] fields, Object obj, Map<String, String> replacements) {
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Replaces annonation = field.getAnnotation(Replaces.class);
                if (annonation != null) {
                    processReplacesAnnotation(annonation, field.get(obj), replacements);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void processMethods(Class cls,Object obj, Map<String, String> replacements) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            method.setAccessible(true);
            try {
                Replaces annonation = method.getAnnotation(Replaces.class);
                if (annonation != null) {
                    method.invoke(obj, null);
                    processReplacesAnnotation(annonation, method.invoke(obj, null), replacements);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                String msg = "Could not invoke Replacement for object, does it have arguments?";
                throw new RuntimeException(msg, e);
            }
        }
    }

    private void processReplacesAnnotation(@NonNull Replaces annotation, Object obj, Map<String, String> replacements) {
        String path = annotation.value();
        if (annotation.type().equals(Type.STRING)) {
            Object o = obj;
            if (o == null) {
                replacements.put(path, "");
                return;
            }
            Template inner = o.getClass().getDeclaredAnnotation(Template.class);
            if (inner != null) { //Hat ein eigenes Template
                replacements.put(path, new TemplateParser().parse(o));
            }
            else { //Hat kein eigenes Template
                replacements.put(path, o.toString());
            }
        }
        else if (annotation.type().equals(Type.OBJECT)) {
            addAllWithPrefix(replacements, readReplacements(obj), path);

        }
    }


    private void addAllWithPrefix(Map<String, String> main, Map<String, String> sub, String prefix) {
        sub.forEach((k, v) -> main.put(addDotToPath(prefix) + k, v));
    }


    private String addDotToPath(String pathIdentifier) {
        if (pathIdentifier.length() > 0 && !pathIdentifier.endsWith(".")) pathIdentifier += ".";
        return pathIdentifier;
    }
}