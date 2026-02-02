from setuptools import setup, find_packages, Distribution

class BinaryDistribution(Distribution):
    """
    Custom distribution class to force setuptools to create a platform-specific wheel.
    
    Since we are including a pre-compiled shared library (.so), this package 
    is NOT "Pure Python". It depends on the specific OS and Architecture 
    (e.g., Linux aarch64). This class ensures the wheel filename reflects that.
    """
    def has_ext_modules(self):
        return True

setup(
    name='dx_rt',
    version='1.1.4',
    description='Python wrapper for DX-RT using pre-compiled shared library',
    author='Runtime',
    author_email='rt@deepx.ai',

    # 1. Source Directory Configuration
    # Tell setuptools that packages are located under the 'src' directory.
    package_dir={'': 'src'},
    
    # Automatically find packages under 'src' (requires __init__.py in folders).
    packages=find_packages(where='src'),

    # 2. Start Inclusion of Non-Python Files (.so)
    # This instructs setuptools to include .so files found inside the package directories.
    package_data={
        # Include all .so files in any package
        '': ['*.so'],
        # Specific example: include .so files specifically in the 'capi' module
        # 'dx_rt.capi': ['*.so'], 
    },
    include_package_data=True,

    # 3. Zip Safety Configuration
    # Must be False. C-extensions or shared libraries cannot be loaded directly 
    # from a compressed zip/egg file. They must be extracted to the filesystem.
    zip_safe=False,

    # 4. Distribution Class
    # Apply the custom class defined above to mark this as a binary distribution.
    distclass=BinaryDistribution,

    # 5. Dependencies
    install_requires=[
        # Add runtime dependencies here, e.g., 'numpy>=1.18.0'
        'numpy'
    ],
    
    python_requires='>=3.8',
)