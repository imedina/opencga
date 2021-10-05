# Using OpenCGA

## &lt;&lt;&lt;&lt;&lt;&lt;&lt; HEAD

description: &gt;- You can interact with your data in OpenCGA in several different ways, find the

### one that suits you most and get started!

> > > > > > > release-2.1.x
> > > > > > >
> > > > > > > ## Using OpenCGA

### Overview <a id="UsingOpenCGA-RESTfulWebServices"></a>

OpenCGA is implemented to provide users with multiple resources to manage and query variant and phenotypic information.

Each method comes with specific advantages: whilst ones are more versatile and allow users to do almost anything, others are less limited in the use but quicker to configure and use.

OpenCGA is a very versatile piece of software and thus we've dedicated special care to allow the final user to explore every possibility. Anyone can explore and choose the client methods which adapts better to their own specific use case.

#### REST Web Services

&lt;&lt;&lt;&lt;&lt;&lt;&lt; HEAD

## All the clients operate with OpenCGA data through a comprehensive and optimised REST web service Application Programming Interface \(API\), which consists of more than 200 web services.The web services are organised based on the main OpenCGA entities \(see [Data Models](./)\); each entity then displays  different layers of operations with a comprehensive set of parameters.

All the clients operate with OpenCGA data through a comprehensive and optimised REST web service Application Programming Interface \(API\), which consists of more than 200 web services.The web services are organised based on the main OpenCGA entities \(see [Data Models](../../overview/data-models/)\); each entity then displays different layers of operations with a comprehensive set of parameters.

> > > > > > > release-2.1.x

The design of the web services has allowed to implement **three different ways** to query and operate with the multiple data stores that compose OpenCGA through the REST API, providing an intuitive way for the user to access the big data variant store.

* [REST Client Libraries](https://app.gitbook.com/@opencb/s/opencga/~/drafts/-MgLnDk3roHbBW3IRKmL/manual/using-opencga/client-libraries): four different client libraries have been implemented to ease the use of REST web services. This allows users to easily integrate OpenCGA in any pipeline. The four libraries are equally functional and fully maintained, these are [_Java_](http://docs.opencb.org/display/opencga/Java)_,_ [_Python_](http://docs.opencb.org/display/opencga/Python) \(available at [PyPI](https://pypi.org/project/pyopencga/)\), [_R_](http://docs.opencb.org/display/opencga/R) and [_JavaScript_](http://docs.opencb.org/display/opencga/JavaScript)
* [Command Line](http://docs.opencb.org/display/opencga/Command+Line): users and administrators can use _**`opencga.sh`**_ command line to query and operate OpenCGA. 
* [IVA Web Application](http://docs.opencb.org/display/opencga/IVA+Web+App): an interactive web application called IVA has been developed to query and visualisation OpenCGA data.

&lt;&lt;&lt;&lt;&lt;&lt;&lt; HEAD

=======

> > > > > > > release-2.1.x

