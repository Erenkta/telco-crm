MapStruct ve Lombok Annotation Processor dediğimiz yapılarla çalışır. Bunlar anotasyonları algılayıp arkada getter setter ve implementor oluşturma işlemlerini yapar. Fakat 2 tane annotation processor varsa sıra önemli hale gelir.

Getter/Setter Lombok tarafından üretiliyor ve MapStruct bu getter/setter'ları kullandığı için öncelikle Lombok'un çalışması gerekir. `annotationProcessorPaths` ile bunu belirtmeliyiz.

![[Pasted image 20260910191651.png]]


-------------------
