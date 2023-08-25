package com.github.wlxxyw.utils;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JaxbUtil {
    public static <T> String object2xml(T obj, Class<T> clazz) throws JAXBException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT,Boolean.TRUE);
        marshaller.setListener(new MarshallerListener());
        marshaller.marshal(obj, byteArrayOutputStream);
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    static class MarshallerListener extends Marshaller.Listener{
        public static final String BLANK_CHAR = "";
        public static final Number BLANK_NUMBER = 0;
        public static final Date BLANK_DATE = new Date(0);
        @Override
        public void beforeMarshal(Object source) {
            super.beforeMarshal(source);
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    if(null == f.get(source)){
                        String className = f.getType().getName();
                        switch (className){
                            case "java.lang.String": f.set(source, BLANK_CHAR);break;
                            case "java.lang.Short": f.set(source, BLANK_NUMBER.shortValue());break;
                            case "java.lang.Integer": f.set(source, BLANK_NUMBER.intValue());break;
                            case "java.lang.Float": f.set(source, BLANK_NUMBER.floatValue());break;
                            case "java.lang.Double": f.set(source, BLANK_NUMBER.doubleValue());break;
                            case "java.util.Date":
                            case "java.sql.Date":
                            case "java.sql.Time": f.set(source, BLANK_DATE);break;
                        }
                    }
                } catch (IllegalAccessException e) {
                    Logger.error(e.getLocalizedMessage(),e);
                }
            }
        }
    }
    public static <T> T xml2Object(String xml, Class<T> clazz) throws JAXBException, XMLStreamException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        XMLStreamReader read = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        JAXBElement<T> jaxbElement = unmarshaller.unmarshal(read,clazz);
        return jaxbElement.getValue();
    }
}
